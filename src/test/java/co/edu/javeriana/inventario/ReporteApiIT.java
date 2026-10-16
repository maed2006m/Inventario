package co.edu.javeriana.inventario;

import co.edu.javeriana.inventario.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de los reportes, el dashboard y la exportacion CSV.
 * Atraviesan filtro JWT, controlador, servicios y la base PostgreSQL real.
 */
class ReporteApiIT extends IntegracionBase {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    private String tokenTendero() {
        return "Bearer " + jwt.generar("tendero", "TENDERO");
    }

    private String tokenAdmin() {
        return "Bearer " + jwt.generar("admin", "ADMIN");
    }

    @Test
    @DisplayName("Admin consulta la rotacion con el rango por defecto")
    void adminConsultaRotacion() throws Exception {
        mvc.perform(get("/api/reportes/rotacion")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin consulta la rotacion con un rango de fechas")
    void adminConsultaRotacionConRango() throws Exception {
        mvc.perform(get("/api/reportes/rotacion")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin consulta la participacion de los productos")
    void adminConsultaParticipacion() throws Exception {
        mvc.perform(get("/api/reportes/participacion")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin consulta el resumen del dashboard")
    void adminConsultaDashboard() throws Exception {
        mvc.perform(get("/api/dashboard/resumen")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin exporta el historial de movimientos en CSV")
    void adminExportaCsv() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/movimientos/export")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andReturn();

        // La respuesta se escribe en streaming: hay que completar el despacho asincrono.
        if (resultado.getRequest().isAsyncStarted()) {
            resultado = mvc.perform(asyncDispatch(resultado)).andReturn();
        }

        assertEquals(200, resultado.getResponse().getStatus());
        String tipo = resultado.getResponse().getContentType();
        assertNotNull(tipo);
        assertTrue(tipo.contains("csv"), "Se esperaba un CSV pero llego: " + tipo);
    }

    @Test
    @DisplayName("El tendero no puede ver reportes ni exportar (403)")
    void tenderoNoPuedeVerReportes() throws Exception {
        mvc.perform(get("/api/reportes/rotacion")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reportes/participacion")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/movimientos/export")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin token los reportes responden 401")
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/reportes/rotacion"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isUnauthorized());
    }
}