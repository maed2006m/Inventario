package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProductoResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.ExistenciaView;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de productos (RF-06 a RF-13, RF-23, RF-24).
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final MovimientoRepository movimientos;

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {

        Proveedor prov = buscarProveedorOFallar(req.proveedorId());

        // RN-06: no se puede dar de alta un producto con un surtidor
        // con el que la tienda ya no trabaja.
        if (!prov.isActivo()) {
            throw new BusinessRuleException(
                    "El proveedor '" + prov.getNombre() + "' esta inactivo.",
                    "Elija un proveedor activo o reactive este.");
        }

        // RN-09: el SKU identifica al producto y no se puede repetir.
        if (productos.existsBySku(req.sku())) {
            throw new BusinessRuleException(
                    "Ya existe un producto con el SKU " + req.sku() + ".",
                    "Use un SKU distinto.");
        }

        Producto p = new Producto();
        aplicar(p, req, prov);

        // Un producto recien creado no tiene movimientos: existencia 0.
        return aRespuesta(productos.save(p), 0);
    }

    /**
     * Listado del catalogo con existencia y estado (RF-24, HU10).
     *
     * Clave de rendimiento: existenciasTodos() trae las existencias de
     * TODO el catalogo en UNA consulta agrupada. La version ingenua
     * (consultar la existencia producto por producto) seria el
     * problema N+1 que el informe debe documentar.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String texto, Long proveedorId, String categoria,
                                         EstadoStock estado, boolean soloActivos) {

        Specification<Producto> spec = ProductoSpecs.filtrar(texto, proveedorId, categoria, soloActivos);

        Map<Long, Long> existencias = movimientos.existenciasTodos().stream()
                .collect(Collectors.toMap(ExistenciaView::getProductoId, ExistenciaView::getExistencia));

        return productos.findAll(spec, Sort.by("nombre")).stream()
                .map(p -> aRespuesta(p, existencias.getOrDefault(p.getId(), 0L).intValue()))
                // El filtro por estado va al final porque el estado se
                // calcula en memoria, no es una columna de la tabla.
                .filter(r -> estado == null || r.estado() == estado)
                .toList();
    }

    /** HU11: productos en BAJO_STOCK o AGOTADO. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> alertas() {
        return listar(null, null, null, null, true).stream()
                .filter(r -> r.estado() != EstadoStock.EN_STOCK)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        Producto p = productos.findWithProveedorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El producto " + id + " no existe."));

        return aRespuesta(p, (int) movimientos.calcularExistencia(id));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {

        Producto p = buscarOFallar(id);

        /*
         * RN-11: el SKU no se puede cambiar si el producto ya tiene
         * movimientos. El SKU es la referencia con la que el historial
         * fue registrado; cambiarlo reescribiria el pasado.
         */
        if (!p.getSku().equals(req.sku()) && movimientos.existsByProductoId(id)) {
            throw new BusinessRuleException(
                    "No se puede cambiar el SKU de un producto que ya tiene movimientos.",
                    "Cree un producto nuevo con el SKU correcto y deshabilite este.");
        }

        if (productos.existsBySkuAndIdNot(req.sku(), id)) {
            throw new BusinessRuleException(
                    "Ya existe otro producto con el SKU " + req.sku() + ".",
                    "Use un SKU distinto.");
        }

        Proveedor prov = buscarProveedorOFallar(req.proveedorId());

        // RN-11: solo se puede reasignar a un proveedor activo.
        if (!prov.isActivo()) {
            throw new BusinessRuleException(
                    "El proveedor '" + prov.getNombre() + "' esta inactivo.",
                    "Elija un proveedor activo.");
        }

        aplicar(p, req, prov);

        return aRespuesta(productos.save(p), (int) movimientos.calcularExistencia(id));
    }

    /** RF-10, RF-11: deshabilitar conserva todos los movimientos. */
    @Transactional
    public void deshabilitar(Long id) {
        Producto p = buscarOFallar(id);
        p.setActivo(false);
        productos.save(p);
    }

    @Transactional
    public ProductoResponse habilitar(Long id) {
        Producto p = buscarOFallar(id);

        if (!p.getProveedor().isActivo()) {
            throw new BusinessRuleException(
                    "No se puede habilitar: el proveedor '" + p.getProveedor().getNombre() + "' esta inactivo.",
                    "Reactive el proveedor o reasigne el producto a uno activo.");
        }

        p.setActivo(true);
        return aRespuesta(productos.save(p), (int) movimientos.calcularExistencia(id));
    }

    /**
     * RN-10: calculo del estado de stock.
     *
     * Es static y sin dependencias para poder probarlo con JUnit sin
     * levantar Spring ni la base de datos.
     */
    public static EstadoStock estadoDe(int existencia, int minimo) {
        if (existencia <= 0) {
            return EstadoStock.AGOTADO;
        }
        return existencia <= minimo ? EstadoStock.BAJO_STOCK : EstadoStock.EN_STOCK;
    }

    private Producto buscarOFallar(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El producto " + id + " no existe."));
    }

    private Proveedor buscarProveedorOFallar(Long id) {
        return proveedores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El proveedor " + id + " no existe."));
    }

    private void aplicar(Producto p, ProductoRequest req, Proveedor prov) {
        p.setSku(req.sku());
        p.setNombre(req.nombre());
        p.setCategoria(req.categoria());
        p.setProveedor(prov);
        p.setStockMinimo(req.stockMinimo());
        p.setCostoUnitario(req.costoUnitario());
        p.setPrecioVenta(req.precioVenta());
    }

    private ProductoResponse aRespuesta(Producto p, int existencia) {
        return new ProductoResponse(
                p.getId(), p.getSku(), p.getNombre(), p.getCategoria(),
                p.getProveedor().getId(), p.getProveedor().getNombre(),
                p.getStockMinimo(), p.getCostoUnitario(), p.getPrecioVenta(),
                p.isActivo(), existencia, estadoDe(existencia, p.getStockMinimo()));
    }
}
