package co.edu.javeriana.inventario.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Formato único de las respuestas de error de la API.
 *
 * @param timestamp momento en que ocurrió el error
 * @param status    código HTTP (400, 404, 409, 500)
 * @param error     nombre estándar del código ("Bad Request", "Conflict", ...)
 * @param message   causa del problema, escrita para el usuario (RNF-03)
 * @param accion    qué puede hacer el usuario para corregirlo (RNF-03)
 * @param path      ruta que se llamó, por ejemplo /api/movimientos
 * @param campos    errores por campo en validaciones (solo en los 400); en los demás casos no se envía
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String accion,
        String path,
        Map<String, String> campos) {
}