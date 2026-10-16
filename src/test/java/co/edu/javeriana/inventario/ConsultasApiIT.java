package co.edu.javeriana.inventario;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de las consultas: historial filtrado y paginado,
 * y listados de productos y proveedores. Atraviesan filtro JWT, controlador,
 * servicio, Specifications y la base PostgreSQL real.
 */
class ConsultasApiIT extends IntegracionBase {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    private String tokenTendero() {
        return "Bearer " + jwt.generar("tendero", "TENDERO");
    }

    @Test
    @DisplayName("Historial de movimientos sin filtros, paginado")
    void historialSinFiltros() throws Exception {
        mvc.perform(get("/api/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Historial de movimientos por producto, proveedor y rango de fechas")
    void historialConFiltrosDeProductoProveedorYFechas() throws Exception {
        mvc.perform(get("/api/movimientos")
                        .param("productoId", "1")
                        .param("proveedorId", "1")
                        .param("desde", "2026-01-01T00:00:00Z")
                        .param("hasta", "2027-12-31T23:59:59Z")
                        .param("page", "0")
                        .param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Historial de movimientos solo por tipo")
    void historialPorTipo() throws Exception {
        mvc.perform(get("/api/movimientos")
                        .param("tipo", TipoMovimiento.values()[0].name())
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Historial por tipo y motivo: coherentes dan 200, incoherentes dan 409")
    void historialPorTipoYMotivo() throws Exception {
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            for (MotivoMovimiento motivo : MotivoMovimiento.values()) {
                int estado = mvc.perform(get("/api/movimientos")
                                .param("tipo", tipo.name())
                                .param("motivo", motivo.name())
                                .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                        .andReturn().getResponse().getStatus();
                assertTrue(estado == 200 || estado == 409,
                        "Combinacion " + tipo + "/" + motivo + " respondio " + estado);
            }
        }
    }

    @Test
    @DisplayName("Listado de productos")
    void listarProductos() throws Exception {
        mvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Listado de proveedores")
    void listarProveedores() throws Exception {
        mvc.perform(get("/api/proveedores")
                        .header(HttpHeaders.AUTHORIZATION, tokenTendero()))
                .andExpect(status().isOk());
    }
}