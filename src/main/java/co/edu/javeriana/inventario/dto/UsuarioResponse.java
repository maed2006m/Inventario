package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.Usuario;

import java.time.OffsetDateTime;

/**
 * Datos publicos de un usuario.
 *
 * Lo devuelven GET /api/auth/me (el frontend lo usa para restaurar la
 * sesion al recargar la pagina) y la gestion de usuarios (RF-30).
 * Nunca incluye el hash de la contrasena.
 */
public record UsuarioResponse(
        Long id,
        String username,
        String nombre,
        String rol,
        boolean activo,
        OffsetDateTime creadoEn
) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getRol().name(),
                u.isActivo(),
                u.getCreadoEn()
        );
    }
}
