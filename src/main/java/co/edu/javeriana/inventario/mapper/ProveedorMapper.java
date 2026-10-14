package co.edu.javeriana.inventario.mapper;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.dto.ProveedorResponse;
import co.edu.javeriana.inventario.model.Proveedor;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el DTO de proveedor y la entidad Proveedor.
 * No contiene reglas de negocio: solo copia campos. Las reglas viven en ProveedorService.
 */
@Component
public class ProveedorMapper {

    /** Crea una entidad nueva a partir de lo que envió el cliente. Nace activa (RN-07). */
    public Proveedor aEntidad(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        copiarCampos(request, proveedor);
        proveedor.setActivo(true);
        return proveedor;
    }

    /** Copia los datos editables sobre un proveedor que ya existe (RF-02). No toca id, activo ni fechas. */
    public void actualizar(Proveedor proveedor, ProveedorRequest request) {
        copiarCampos(request, proveedor);
    }

    /** Convierte la entidad en la respuesta que ve el cliente (RF-05). */
    public ProveedorResponse aRespuesta(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getNit(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.getTiempoEntregaDias(),
                proveedor.isActivo(),
                proveedor.getCreadoEn(),
                proveedor.getActualizadoEn());
    }

    /** Los seis campos que el cliente puede enviar. Se escribe una sola vez para crear y editar. */
    private void copiarCampos(ProveedorRequest request, Proveedor proveedor) {
        proveedor.setNombre(request.nombre());
        proveedor.setNit(request.nit());
        proveedor.setTelefono(request.telefono());
        proveedor.setEmail(request.email());
        proveedor.setDireccion(request.direccion());
        proveedor.setTiempoEntregaDias(request.tiempoEntregaDias());
    }
}