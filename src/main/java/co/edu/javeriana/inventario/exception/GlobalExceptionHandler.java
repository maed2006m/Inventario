package co.edu.javeriana.inventario.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de toda la aplicacion a respuestas HTTP.
 *
 * Centralizarlo aqui evita tener try/catch en cada controlador y
 * garantiza que el formato del error sea siempre el mismo.
 *
 * IMPORTANTE: aqui NO se manejan AccessDeniedException ni
 * AuthenticationException. Esas las resuelve Spring Security antes
 * de llegar al controlador (ver SecurityConfig), y si las
 * capturaramos aqui los 401 y 403 dejarian de funcionar.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 - el recurso pedido no existe. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> noEncontrado(ResourceNotFoundException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(),
                "Verifique el identificador enviado.", r, null);
    }

    /** 409 - la peticion es valida pero choca con una regla de negocio. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> reglaDeNegocio(BusinessRuleException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), e.getAccion(), r, null);
    }

/** 400 - la solicitud es invalida (por ejemplo, rango de fechas invertido). */
@ExceptionHandler(SolicitudInvalidaException.class)
public ResponseEntity<ApiError> solicitudInvalida(SolicitudInvalidaException e, HttpServletRequest r) {
    return build(HttpStatus.BAD_REQUEST, e.getMessage(), e.getAccion(), r, null);
}


    /** 405 - la ruta existe pero no admite ese metodo HTTP (por ejemplo, editar un movimiento). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoPermitido(HttpRequestMethodNotSupportedException e,
                                                      HttpServletRequest r) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "El metodo " + e.getMethod() + " no esta permitido en esta ruta.",
                "Los movimientos son inmutables: solo se pueden registrar y consultar.", r, null);
    }


    /** 401 - credenciales invalidas en el login. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> credenciales(CredencialesInvalidasException e, HttpServletRequest r) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(),
                "Verifique su usuario y contrasena.", r, null);
    }

    /**
     * 400 - fallo la validacion de @Valid sobre el DTO de entrada.
     *
     * Sin este metodo Spring devuelve un JSON largo donde los mensajes
     * personalizados quedan enterrados. Aqui los extraemos a un mapa
     * campo -> motivo, que es lo que el frontend puede mostrar junto
     * a cada input del formulario.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));

        return build(HttpStatus.BAD_REQUEST, "Hay datos invalidos en la solicitud.",
                "Corrija los campos indicados y reenvie.", r, campos);
    }

    /**
     * 409 - una restriccion de la base de datos rechazo la operacion.
     *
     * Es la red de seguridad: si un UNIQUE o un CHECK se dispara sin
     * que el servicio lo haya previsto, el usuario recibe un 409 con
     * sentido en lugar de un 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, "La operacion viola una restriccion de la base de datos.",
                "Revise los valores unicos (SKU, NIT) y las referencias.", r, null);
    }

    /**
     * 500 - cualquier cosa no prevista.
     *
     * Debe quedar de ultimo. No expone la traza ni el nombre de la
     * clase de la excepcion: eso es informacion util para un atacante
     * e inutil para el usuario.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception e, HttpServletRequest r) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.",
                "Intente de nuevo; si persiste, contacte al administrador.", r, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje, String accion,
                                           HttpServletRequest request, Map<String, String> campos) {
        ApiError cuerpo = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                accion,
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.status(status).body(cuerpo);
    }
}
