package co.edu.javeriana.inventario.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP con el formato ApiError.
 * Cada respuesta indica la causa y la acción correctiva (RNF-03).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404: el recurso buscado no existe. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(ResourceNotFoundException e, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage(),
                "Verifique el identificador e intente de nuevo.", request, null);
    }

    /** 409: una regla de negocio no permite la operación (incluye StockInsuficienteException). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> manejarReglaDeNegocio(BusinessRuleException e, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, e.getMessage(), e.getAccion(), request, null);
    }

    /** 400: los datos enviados no cumplen las validaciones (@Valid). Devuelve los campos con error. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> manejarValidacion(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.",
                "Corrija los campos indicados y vuelva a enviar.", request, campos);
    }

    /** 400: el JSON está mal formado o un valor no es válido (por ejemplo, un tipo de movimiento inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no se pudo interpretar.",
                "Revise que el JSON esté bien formado y que los valores sean válidos (por ejemplo, tipo ENTRADA o SALIDA).",
                request, null);
    }

    /** 400: un parámetro de la URL tiene un tipo incorrecto (por ejemplo, un id que no es número). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> manejarParametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' tiene un valor inválido.",
                "Envíe un valor del tipo correcto.", request, null);
    }

    /** 405: se intentó un método HTTP no permitido, por ejemplo editar o borrar un movimiento (RN-05). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "El método " + e.getMethod() + " no está permitido en esta ruta.",
                "Consulte la documentación de la API en /swagger-ui.html.", request, null);
    }

    /** 409: la base de datos rechazó la operación (valor único repetido, referencia inexistente). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarIntegridad(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Violación de integridad en {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos.",
                "Revise valores únicos (SKU, NIT) y referencias a otros registros.", request, null);
    }

    /** 500: cualquier error no previsto. No se expone la causa interna al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarInesperado(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno del servidor.",
                "Intente de nuevo; si el problema continúa, contacte al administrador.", request, null);
    }

    /** Arma la respuesta con el formato único ApiError. */
    private ResponseEntity<ApiError> construir(HttpStatus estado, String mensaje, String accion,
                                               HttpServletRequest request, Map<String, String> campos) {
        ApiError cuerpo = new ApiError(Instant.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, accion, request.getRequestURI(), campos);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}