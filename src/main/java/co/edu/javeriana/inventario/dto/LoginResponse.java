package co.edu.javeriana.inventario.dto;

/**
 * Respuesta del login.
 *
 * Devuelve el token y los datos minimos que el frontend necesita
 * para armar el menu segun el rol, sin tener que decodificar el JWT.
 *
 * Nunca incluye el hash de la contrasena ni el id interno.
 */
public record LoginResponse(
        String token,
        String username,
        String nombre,
        String rol,
        long expiraEnMinutos
) {
}
