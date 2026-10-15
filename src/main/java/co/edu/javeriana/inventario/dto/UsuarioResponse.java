package co.edu.javeriana.inventario.dto;

/**
 * Datos del usuario autenticado, devueltos por GET /api/auth/me.
 * El frontend lo usa para restaurar la sesion al recargar la pagina.
 */
public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        String rol
) {
}
