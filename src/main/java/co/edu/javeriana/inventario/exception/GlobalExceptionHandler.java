package co.edu.javeriana.inventario.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Tope de caracteres del valor recibido que se repite en el mensaje de error. */
    static final int MAX_VALOR_EN_MENSAJE = 40;

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
     * 400 - el cuerpo no se pudo convertir al DTO (RNF-03).
     *
     * Cubre JSON mal formado, cuerpo vacio y valores del tipo equivocado
     * ("tipo": "FOO", "cantidad": "diez"). Cuando Jackson sabe que campo
     * fallo, se senala en 'campos' igual que en una validacion de @Valid,
     * para que el frontend lo muestre junto al input.
     *
     * No se devuelve e.getMessage(): el texto de Jackson incluye nombres
     * de clases internas del proyecto.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest r) {
        log.warn("Cuerpo ilegible en {} {}", r.getMethod(), r.getRequestURI());

        if (e.getCause() instanceof MismatchedInputException m && !m.getPath().isEmpty()) {
            String detalle = m instanceof InvalidFormatException f
                    ? valorInvalido(f.getValue(), f.getTargetType())
                    : "El tipo de dato enviado no es valido para este campo.";
            return build(HttpStatus.BAD_REQUEST, "Hay datos invalidos en la solicitud.",
                    "Corrija los campos indicados y reenvie.", r, Map.of(rutaDe(m.getPath()), detalle));
        }

        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud esta vacio o no es un JSON valido.",
                "Revise el formato de los datos enviados y reenvie.", r, null);
    }

    /**
     * 400 - un parametro de la URL no tiene el tipo esperado (RNF-03):
     * un id que no es numero, un enum desconocido o una fecha mal escrita.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest r) {
        log.warn("Parametro '{}' invalido en {} {}", e.getName(), r.getMethod(), r.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Hay parametros invalidos en la solicitud.",
                "Corrija los parametros indicados y reenvie.", r,
                Map.of(e.getName(), valorInvalido(e.getValue(), e.getRequiredType())));
    }

    /** 404 - la ruta no corresponde a ningun endpoint de la API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaInexistente(NoResourceFoundException e, HttpServletRequest r) {
        log.warn("Ruta inexistente: {} {}", r.getMethod(), r.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "La ruta " + r.getRequestURI() + " no existe.",
                "Verifique la URL; las rutas disponibles estan en /swagger-ui.html.", r, null);
    }

    /** 415 - el cuerpo no viene como JSON. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> tipoDeContenidoNoSoportado(HttpMediaTypeNotSupportedException e,
                                                               HttpServletRequest r) {
        log.warn("Content-Type no soportado en {} {}: {}", r.getMethod(), r.getRequestURI(), e.getContentType());
        String mensaje = e.getContentType() != null
                ? "El tipo de contenido " + e.getContentType() + " no esta soportado."
                : "El tipo de contenido de la solicitud no esta soportado.";
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, mensaje,
                "Envie los datos con Content-Type: application/json.", r, null);
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
        log.error("Error no controlado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.",
                "Intente de nuevo; si persiste, contacte al administrador.", r, null);
    }

    /** Ruta del campo que fallo, por ejemplo "tipo" o "items[2].cantidad". */
    static String rutaDe(List<JsonMappingException.Reference> ruta) {
        StringBuilder campo = new StringBuilder();
        for (JsonMappingException.Reference ref : ruta) {
            if (ref.getFieldName() != null) {
                campo.append(campo.isEmpty() ? "" : ".").append(ref.getFieldName());
            } else {
                campo.append('[').append(ref.getIndex()).append(']');
            }
        }
        return campo.toString();
    }

    /**
     * Explica por que un valor no sirve y que se esperaba: los valores
     * permitidos de un enum, el formato de una fecha o que debe ser un
     * numero. El valor recibido se recorta para no devolver textos enormes.
     */
    static String valorInvalido(Object valor, Class<?> tipo) {
        String texto = String.valueOf(valor);
        if (texto.length() > MAX_VALOR_EN_MENSAJE) {
            texto = texto.substring(0, MAX_VALOR_EN_MENSAJE) + "...";
        }
        String base = "El valor '" + texto + "' no es valido.";

        if (tipo == null) {
            return base;
        }
        if (tipo.isEnum()) {
            String permitidos = Arrays.stream(tipo.getEnumConstants())
                    .map(c -> ((Enum<?>) c).name())
                    .collect(Collectors.joining(", "));
            return base + " Valores permitidos: " + permitidos + ".";
        }
        if (tipo == LocalDate.class) {
            return base + " Use el formato AAAA-MM-DD.";
        }
        if (Temporal.class.isAssignableFrom(tipo)) {
            return base + " Use el formato AAAA-MM-DDTHH:MM:SSZ.";
        }
        if (Number.class.isAssignableFrom(tipo)) {
            return base + " Se espera un numero.";
        }
        return base;
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
