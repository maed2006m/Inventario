package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.dto.ProveedorResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de proveedores (RF-01 a RF-05).
 */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;

    @Transactional
    public ProveedorResponse crear(ProveedorRequest req) {

        // RN-09: el NIT identifica al proveedor y no se puede repetir.
        if (proveedores.existsByNit(req.nit())) {
            throw new BusinessRuleException(
                    "Ya existe un proveedor con el NIT " + req.nit() + ".",
                    "Verifique el NIT o busque el proveedor existente.");
        }

        Proveedor p = new Proveedor();
        aplicar(p, req);

        return aRespuesta(proveedores.save(p), 0);
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar(boolean soloActivos) {

        List<Proveedor> lista = soloActivos
                ? proveedores.findByActivoTrueOrderByNombre()
                : proveedores.findAllByOrderByNombre();

        return lista.stream()
                .map(p -> aRespuesta(p, productos.countByProveedorIdAndActivoTrue(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        Proveedor p = buscarOFallar(id);
        return aRespuesta(p, productos.countByProveedorIdAndActivoTrue(id));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest req) {

        Proveedor p = buscarOFallar(id);

        // Mismo control de unicidad, excluyendo el propio registro.
        if (proveedores.existsByNitAndIdNot(req.nit(), id)) {
            throw new BusinessRuleException(
                    "Ya existe otro proveedor con el NIT " + req.nit() + ".",
                    "Use un NIT distinto.");
        }

        aplicar(p, req);

        return aRespuesta(proveedores.save(p), productos.countByProveedorIdAndActivoTrue(id));
    }

    /**
     * RN-07, RF-03, RF-04: "eliminar" es inactivar.
     *
     * No se borra la fila porque los movimientos historicos apuntan a
     * los productos de este proveedor. Y no se permite inactivarlo
     * mientras tenga productos activos, porque esos productos
     * quedarian huerfanos de un surtidor vigente.
     */
    @Transactional
    public void inactivar(Long id) {

        Proveedor p = buscarOFallar(id);

        long activos = productos.countByProveedorIdAndActivoTrue(id);
        if (activos > 0) {
            throw new BusinessRuleException(
                    "No se puede inactivar a '" + p.getNombre() + "': tiene "
                            + activos + " producto(s) activo(s).",
                    "Deshabilite o reasigne esos productos y vuelva a intentarlo.");
        }

        p.setActivo(false);
        proveedores.save(p);
    }

    private Proveedor buscarOFallar(Long id) {
        return proveedores.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El proveedor " + id + " no existe."));
    }

    private void aplicar(Proveedor p, ProveedorRequest req) {
        p.setNombre(req.nombre());
        p.setNit(req.nit());
        p.setTelefono(req.telefono());
        p.setEmail(req.email());
        p.setDireccion(req.direccion());
        p.setTiempoEntregaDias(req.tiempoEntregaDias());
    }

    private ProveedorResponse aRespuesta(Proveedor p, long productosActivos) {
        return new ProveedorResponse(
                p.getId(), p.getNombre(), p.getNit(), p.getTelefono(),
                p.getEmail(), p.getDireccion(), p.getTiempoEntregaDias(),
                p.isActivo(), productosActivos);
    }
}
