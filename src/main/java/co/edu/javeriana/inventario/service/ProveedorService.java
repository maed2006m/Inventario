package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.dto.ProveedorResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.mapper.ProveedorMapper;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de proveedores (RF-01 a RF-05).
 * Aplica RN-09 (NIT único) y RN-07 (eliminación lógica con protección de productos activos).
 */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final ProveedorMapper mapper;

    /** Lista los proveedores ordenados por nombre. Con soloActivos=true oculta los inactivos (RF-05). */
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar(boolean soloActivos) {
        List<Proveedor> lista = soloActivos
                ? proveedores.findByActivoTrueOrderByNombreAsc()
                : proveedores.findAll(Sort.by("nombre"));
        return lista.stream().map(mapper::aRespuesta).toList();
    }

    /** Consulta un proveedor por id (RF-05). */
    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return mapper.aRespuesta(buscar(id));
    }

    /** Crea un proveedor activo. RN-09: el NIT no puede estar repetido (RF-01). */
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        if (proveedores.existsByNit(request.nit())) {
            throw new BusinessRuleException(
                    "Ya existe un proveedor con el NIT " + request.nit() + ".",
                    "Verifique el NIT o edite el proveedor existente.");
        }
        Proveedor guardado = proveedores.save(mapper.aEntidad(request));
        return mapper.aRespuesta(guardado);
    }

    /** Edita los datos de un proveedor. RN-09: el NIT no puede coincidir con el de otro proveedor (RF-02). */
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        if (proveedores.existsByNitAndIdNot(request.nit(), id)) {
            throw new BusinessRuleException(
                    "El NIT " + request.nit() + " ya pertenece a otro proveedor.",
                    "Use un NIT distinto o revise el proveedor que lo tiene registrado.");
        }
        mapper.actualizar(proveedor, request);
        return mapper.aRespuesta(proveedor);
    }

    /**
     * Eliminación lógica (RF-03, RF-04). El proveedor queda inactivo y se conserva todo su historial.
     * RN-07: no se puede inactivar mientras tenga productos activos.
     */
    @Transactional
    public void inactivar(Long id) {
        Proveedor proveedor = buscar(id);
        long activos = productos.countByProveedorIdAndActivoTrue(id);
        if (activos > 0) {
            throw new BusinessRuleException(
                    "No se puede inactivar a '" + proveedor.getNombre() + "': tiene " + activos + " productos activos.",
                    "Deshabilite o reasigne esos productos y vuelva a intentarlo.");
        }
        proveedor.setActivo(false);
    }

    /** Busca un proveedor o lanza el 404. Lo usan los demás métodos de la clase. */
    private Proveedor buscar(Long id) {
        return proveedores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El proveedor " + id + " no existe."));
    }
}