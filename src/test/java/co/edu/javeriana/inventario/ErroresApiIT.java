package co.edu.javeriana.inventario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Solicitudes mal formadas (RNF-03).
 *
 * Antes caian en el manejador generico y respondian 500, como si fuera
 * un fallo del servidor. Son errores del cliente: deben responder 400,
 * 404 o 415 con el mismo ApiError que el resto de la API (message,
 * accion y, si se sabe, el campo en 'campos'), sin exponer nombres de
 * clases internas.
 */
class ErroresApiIT extends ApiIntegracionBase {

    private ResultActions postMovimiento(String cuerpo) throws Exception {
        return mvc.perform(post("/api/movimientos")
                .header(HttpHeaders.AUTHORIZATION, tokenTendero())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions consultar(String ruta) throws Exception {
        return mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, tokenAdmin()));
    }

    // ---------- cuerpo ilegible ----------

    @Test
    @DisplayName("RNF-03: un JSON mal formado responde 400 sin exponer detalles internos")
    void jsonMalFormado() throws Exception {
        postMovimiento("{\"productoId\": 1, \"tipo\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud esta vacio o no es un JSON valido."))
                .andExpect(jsonPath("$.accion").isNotEmpty())
                .andExpect(jsonPath("$.campos").doesNotExist())
                .andExpect(content().string(not(containsString("jackson"))));
    }

    @Test
    @DisplayName("RNF-03: un POST sin cuerpo responde 400")
    void cuerpoVacio() throws Exception {
        postMovimiento("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RNF-03: un JSON que no es un objeto responde 400 con el mensaje general")
    void cuerpoQueNoEsObjeto() throws Exception {
        postMovimiento("[1, 2, 3]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    @DisplayName("RNF-03: un enum invalido en el cuerpo senala el campo y los valores permitidos")
    void enumInvalidoEnElCuerpo() throws Exception {
        postMovimiento("{\"productoId\":1,\"tipo\":\"FOO\",\"motivo\":\"COMPRA\",\"cantidad\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").value(
                        "El valor 'FOO' no es valido. Valores permitidos: ENTRADA, SALIDA."))
                .andExpect(content().string(not(containsString("co.edu.javeriana"))));
    }

    @Test
    @DisplayName("RNF-03: texto donde se espera un numero senala el campo")
    void textoEnCampoNumerico() throws Exception {
        postMovimiento("{\"productoId\":1,\"tipo\":\"ENTRADA\",\"motivo\":\"COMPRA\",\"cantidad\":\"diez\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").value("El valor 'diez' no es valido. Se espera un numero."));
    }

    @Test
    @DisplayName("RNF-03: un objeto donde se espera un valor simple senala el campo")
    void objetoEnCampoSimple() throws Exception {
        postMovimiento("{\"productoId\":{\"id\":1},\"tipo\":\"ENTRADA\",\"motivo\":\"COMPRA\",\"cantidad\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.productoId").value("El tipo de dato enviado no es valido para este campo."));
    }

    @Test
    @DisplayName("RNF-03: un valor muy largo se recorta en el mensaje de error")
    void valorLargoSeRecorta() throws Exception {
        String largo = "X".repeat(500);
        postMovimiento("{\"productoId\":1,\"tipo\":\"" + largo + "\",\"motivo\":\"COMPRA\",\"cantidad\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").value(allOf(
                        containsString("X".repeat(40) + "..."),
                        not(containsString("X".repeat(41))))));
    }

    // ---------- parametros de la URL ----------

    @Test
    @DisplayName("RNF-03: un enum invalido en la URL responde 400 con los valores permitidos")
    void enumInvalidoEnLaUrl() throws Exception {
        consultar("/api/movimientos?motivo=REGALO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hay parametros invalidos en la solicitud."))
                .andExpect(jsonPath("$.campos.motivo").value(
                        "El valor 'REGALO' no es valido. Valores permitidos: COMPRA, VENTA, AVERIA, VENCIMIENTO."));
    }

    @Test
    @DisplayName("RNF-03: un id que no es numero responde 400")
    void idNoNumerico() throws Exception {
        consultar("/api/productos/abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.id").value("El valor 'abc' no es valido. Se espera un numero."));
    }

    @Test
    @DisplayName("RNF-03: una fecha mal escrita indica el formato esperado")
    void fechaMalEscrita() throws Exception {
        consultar("/api/movimientos?desde=ayer")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.desde").value(containsString("AAAA-MM-DDTHH:MM:SSZ")));

        // Los reportes usan fechas sin hora
        consultar("/api/reportes/rotacion?desde=junio")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.desde").value(containsString("AAAA-MM-DD.")));
    }

    @Test
    @DisplayName("RNF-03: un booleano invalido responde 400 senalando el parametro")
    void booleanoInvalido() throws Exception {
        consultar("/api/productos?soloActivos=quizas")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.soloActivos").value("El valor 'quizas' no es valido."));
    }

    // ---------- ruta y tipo de contenido ----------

    @Test
    @DisplayName("RNF-03: una ruta inexistente responde 404; sin token sigue respondiendo 401")
    void rutaInexistente() throws Exception {
        consultar("/api/noexiste")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La ruta /api/noexiste no existe."))
                .andExpect(jsonPath("$.accion").isNotEmpty());

        // La seguridad se evalua antes: no se revela que rutas existen
        mvc.perform(get("/api/noexiste"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RNF-03: un cuerpo que no es JSON responde 415 indicando el Content-Type correcto")
    void contenidoNoSoportado() throws Exception {
        mvc.perform(post("/api/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value(containsString("text/plain")))
                .andExpect(jsonPath("$.accion").value("Envie los datos con Content-Type: application/json."));
    }
}
