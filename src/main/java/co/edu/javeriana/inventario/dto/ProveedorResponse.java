package co.edu.javeriana.inventario.dto;

/**
 * Proveedor tal como lo devuelve la API.
 *
 * productosActivos permite al frontend avisar por que no se puede
 * inactivar un proveedor antes de siquiera intentarlo (RN-07).
 */
public record ProveedorResponse(
        Long id,
        String nombre,
        String nit,
        String telefono,
        String email,
        String direccion,
        Integer tiempoEntregaDias,
        boolean activo,
        long productosActivos
) {
}
