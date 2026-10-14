package co.edu.javeriana.inventario.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo unificado de todas las respuestas de error de la API.
 *
 * Que todos los errores tengan la misma forma permite que el frontend
 * los maneje con un solo bloque de codigo, en lugar de adivinar si
 * recibio un texto plano o un JSON.
 *
 * - message: que paso, en lenguaje del usuario (no una traza tecnica)
 * - accion:  que debe hacer para corregirlo (RNF-03)
 * - campos:  solo en errores de validacion, campo -> motivo
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String accion,
        String path,
        Map<String, String> campos
) {
}
