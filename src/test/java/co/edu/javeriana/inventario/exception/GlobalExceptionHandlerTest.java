package co.edu.javeriana.inventario.exception;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Respuestas de GlobalExceptionHandler que no se pueden provocar de forma
 * fiable por la API: la violacion de integridad que el servicio no previo
 * (por ejemplo, dos altas simultaneas con el mismo SKU), el error
 * inesperado y los casos de solicitudes mal formadas que ningun DTO
 * actual permite (listas, Content-Type ausente) (RNF-03). Los casos
 * alcanzables por la API estan en ErroresApiIT.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static MockHttpServletRequest peticion(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }

    @Test
    @DisplayName("RNF-03: una violacion de integridad de la base responde 409 con accion correctiva")
    void violacionDeIntegridad() {
        ResponseEntity<ApiError> r = handler.integridad(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_sku\""),
                peticion("/api/productos"));

        assertEquals(409, r.getStatusCode().value());
        ApiError cuerpo = r.getBody();
        assertNotNull(cuerpo);
        assertEquals("/api/productos", cuerpo.path());
        assertNotNull(cuerpo.accion());
        assertFalse(cuerpo.message().contains("uk_sku"), "No debe exponer el detalle de la base");
    }

    @Test
    @DisplayName("RNF-03: la ruta del campo incluye objetos anidados e indices de listas")
    void rutaDeCampoAnidado() {
        Object raiz = new Object();
        List<JsonMappingException.Reference> ruta = List.of(
                new JsonMappingException.Reference(raiz, "items"),
                new JsonMappingException.Reference(raiz, 2),
                new JsonMappingException.Reference(raiz, "cantidad"));

        assertEquals("items[2].cantidad", GlobalExceptionHandler.rutaDe(ruta));
    }

    @Test
    @DisplayName("RNF-03: sin tipo esperado conocido solo se informa el valor invalido")
    void valorInvalidoSinTipo() {
        assertEquals("El valor 'x' no es valido.", GlobalExceptionHandler.valorInvalido("x", null));
        assertEquals("El valor 'null' no es valido.", GlobalExceptionHandler.valorInvalido(null, String.class));
    }

    @Test
    @DisplayName("RNF-03: un error de formato en un campo de una lista senala el elemento")
    void cuerpoIlegibleEnElementoDeLista() {
        InvalidFormatException causa = InvalidFormatException.from(null, "formato", "FOO", MotivoMovimiento.class);
        causa.prependPath(new Object(), "motivo");
        causa.prependPath(new Object(), 0);

        ResponseEntity<ApiError> r = handler.cuerpoIlegible(
                new HttpMessageNotReadableException("ilegible", causa, new MockHttpInputMessage(new byte[0])),
                peticion("/api/movimientos"));

        assertEquals(400, r.getStatusCode().value());
        assertNotNull(r.getBody());
        assertTrue(r.getBody().campos().get("[0].motivo").contains("COMPRA, VENTA, AVERIA, VENCIMIENTO"));
    }

    @Test
    @DisplayName("RNF-03: sin Content-Type la respuesta 415 usa un mensaje general")
    void contenidoSinTipo() {
        ResponseEntity<ApiError> r = handler.tipoDeContenidoNoSoportado(
                new HttpMediaTypeNotSupportedException("sin tipo"), peticion("/api/movimientos"));

        assertEquals(415, r.getStatusCode().value());
        assertNotNull(r.getBody());
        assertEquals("El tipo de contenido de la solicitud no esta soportado.", r.getBody().message());
    }

    @Test
    @DisplayName("RNF-03: un error inesperado responde 500 sin exponer la causa")
    void errorInesperado() {
        ResponseEntity<ApiError> r = handler.inesperado(
                new IllegalStateException("NullPointer en ClaseInterna.metodo"),
                peticion("/api/movimientos"));

        assertEquals(500, r.getStatusCode().value());
        ApiError cuerpo = r.getBody();
        assertNotNull(cuerpo);
        assertEquals("Error interno del servidor.", cuerpo.message());
        assertNotNull(cuerpo.accion());
        assertNull(cuerpo.campos());
    }
}
