package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProductoResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.mapper.ProductoMapper;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProductoSpecs;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de productos (RF-06 a RF-13, RF-20, RF-23, RF-24).
 * Aplica RN-01, RN-06, RN-09, RN-10 y RN-11.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final MovimientoRepository movimientos;
    private final ProductoMapper mapper;

    /**
     * Lista productos con existencia y estado calculados (RF-20, RF-23, RF-24).
     * La existencia de TODOS los productos se obtiene en una sola consulta agrupada (RNF-06).
     * El filtro por estado se aplica en memoria porque el estado no es una columna de la base.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String q, Long proveedorId, String categoria,
                                         EstadoStock estado, boolean soloActivos) {
        Map<Long, Long> existencias = movimientos.existenciasTodos().stream()
                .collect(Collectors.toMap(
                        MovimientoRepository.ExistenciaView::getProductoId,
                        MovimientoRepository.ExistenciaView::getExistencia));

        return productos.findAll(ProductoSpecs.filtrar(q, proveedorId, categoria, soloActivos), Sort.by("nombre"))
                .stream()
                .map(p -> aRespuesta(p, existencias.getOrDefault(p.getId(), 0L).intValue()))
                .filter(r -> estado == null || r.estado() == estado)
                .toList();
    }

    /** Productos activos agotados o por debajo del mínimo, los más urgentes primero (RF-23, HU11). */
    @Transactional(readOnly = true)
    public List<ProductoResponse> alertas() {
        return listar(null, null, null, null, true).stream()
                .filter(r -> r.estado() != EstadoStock.EN_STOCK)
                .sorted(Comparator.comparing(ProductoResponse::estado)
                        .thenComparing(ProductoResponse::existencia))
                .toList();
    }

    /** Consulta un producto con su existencia actual (RF-12, RF-20). */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        Producto producto = buscar(id);
        return aRespuesta(producto, (int) movimientos.calcularExistencia(id));
    }

    /** Crea un producto activo con existencia 0 (RF-06). Aplica RN-06 y RN-09. */
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Proveedor proveedor = buscarProveedor(request.proveedorId());
        validarProveedorActivo(proveedor);                                   // RN-06
        if (productos.existsBySku(request.sku())) {                          // RN-09
            throw new BusinessRuleException(
                    "Ya existe un producto con el SKU " + request.sku() + ".",
                    "Use un SKU distinto.");
        }
        Producto guardado = productos.save(mapper.aEntidad(request, proveedor));
        return aRespuesta(guardado, 0);
    }

    /** Edita un producto (RF-09). Aplica RN-09 y RN-11. La existencia no se puede modificar (RN-01). */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);

        boolean cambiaSku = !producto.getSku().equals(request.sku());
        if (cambiaSku) {
            if (movimientos.existsByProductoId(id)) {                        // RN-11
                throw new BusinessRuleException(
                        "No se puede cambiar el SKU de '" + producto.getNombre() + "': ya tiene movimientos.",
                        "Conserve el SKU original o cree un producto nuevo.");
            }
            if (productos.existsBySkuAndIdNot(request.sku(), id)) {          // RN-09
                throw new BusinessRuleException(
                        "El SKU " + request.sku() + " ya pertenece a otro producto.",
                        "Use un SKU distinto.");
            }
        }

        Proveedor proveedor = producto.getProveedor();
        if (!proveedor.getId().equals(request.proveedorId())) {              // RN-11
            proveedor = buscarProveedor(request.proveedorId());
            validarProveedorActivo(proveedor);
        }

        mapper.actualizar(producto, request, proveedor);
        return aRespuesta(producto, (int) movimientos.calcularExistencia(id));
    }

    /** Eliminación lógica: el producto queda deshabilitado y conserva su historial (RF-10, RF-11). */
    @Transactional
    public void deshabilitar(Long id) {
        buscar(id).setActivo(false);
    }

    /** Vuelve a habilitar un producto (RF-10). Su proveedor debe estar activo (RN-06). */
    @Transactional
    public ProductoResponse habilitar(Long id) {
        Producto producto = buscar(id);
        if (!producto.getProveedor().isActivo()) {
            throw new BusinessRuleException(
                    "No se puede habilitar '" + producto.getNombre() + "': su proveedor '"
                            + producto.getProveedor().getNombre() + "' está inactivo.",
                    "Edite el producto y asígnele un proveedor activo.");
        }
        producto.setActivo(true);
        return aRespuesta(producto, (int) movimientos.calcularExistencia(id));
    }

    /**
     * RN-10: estado de stock según la existencia y el stock mínimo.
     * Visible dentro del paquete para poder probarlo con pruebas unitarias.
     */
    static EstadoStock estadoDe(int existencia, int stockMinimo) {
        if (existencia == 0) {
            return EstadoStock.AGOTADO;
        }
        return existencia <= stockMinimo ? EstadoStock.BAJO_STOCK : EstadoStock.EN_STOCK;
    }

    private ProductoResponse aRespuesta(Producto producto, int existencia) {
        return mapper.aRespuesta(producto, existencia, estadoDe(existencia, producto.getStockMinimo()));
    }

    private Producto buscar(Long id) {
        return productos.findByIdConProveedor(id)
                .orElseThrow(() -> new ResourceNotFoundException("El producto " + id + " no existe."));
    }

    private Proveedor buscarProveedor(Long id) {
        return proveedores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El proveedor " + id + " no existe."));
    }

    /** RN-06: un producto solo puede asociarse a un proveedor activo. */
    private void validarProveedorActivo(Proveedor proveedor) {
        if (!proveedor.isActivo()) {
            throw new BusinessRuleException(
                    "El proveedor '" + proveedor.getNombre() + "' está inactivo.",
                    "Elija un proveedor activo.");
        }
    }
}