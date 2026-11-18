package co.edu.javeriana.inventario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion del historial de movimientos (HU09, RF-23, RN-15)
 * con datos propios.
 *
 * ConsultasApiIT verifica que ninguna combinacion de filtros responda 500;
 * esta clase verifica el contenido: que el historial devuelva los
 * movimientos registrados con sus datos y que el rango de fechas se valide.
 */
class HistorialApiIT extends ApiIntegracionBase {

    private static String haceHoras(long horas) {
        return Instant.now().minus(Duration.ofHours(horas)).toString();
    }

    @Test
    @DisplayName("HU09: el historial de un producto devuelve sus movimientos con producto, proveedor y usuario")
    void historialConMovimientos() throws Exception {
        long proveedorId = crearProveedor();
        long id = crearProducto(proveedorId, "HIS-" + sufijo(), null, 1);

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "ENTRADA", "motivo", "COMPRA", "cantidad", 10,
                        "valorUnitario", 900, "observacion", "Factura 123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorUnitario").value(900));
        registrar(id, "SALIDA", "VENTA", 3).andExpect(status().isCreated());

        String rango = "&desde=" + haceHoras(1) + "&hasta=" + Instant.now().plus(Duration.ofHours(1));

        enviar(HttpMethod.GET, "/api/movimientos?productoId=" + id + rango, tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].tipo", containsInAnyOrder("ENTRADA", "SALIDA")))
                .andExpect(jsonPath("$.content[*].observacion", containsInAnyOrder("Factura 123", null)))
                .andExpect(jsonPath("$.content[0].productoId").value(id))
                .andExpect(jsonPath("$.content[0].proveedorNombre").value("Proveedor IT"))
                .andExpect(jsonPath("$.content[0].usuario").value("tendero"))
                // El historial no calcula la existencia resultante de cada fila
                .andExpect(jsonPath("$.content[0].existenciaResultante").doesNotExist());

        enviar(HttpMethod.GET, "/api/movimientos?proveedorId=" + proveedorId + "&tipo=SALIDA&motivo=VENTA",
                tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cantidad").value(3));
    }

    @Test
    @DisplayName("RN-15: un rango que termina antes de los movimientos no los devuelve")
    void historialFueraDeRango() throws Exception {
        long id = crearProducto();
        registrar(id, "ENTRADA", "COMPRA", 5).andExpect(status().isCreated());

        enviar(HttpMethod.GET, "/api/movimientos?productoId=" + id
                        + "&desde=" + haceHoras(48) + "&hasta=" + haceHoras(24),
                tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("RN-15: una fecha inicial posterior a la final responde 409 con accion correctiva")
    void historialConFechasInvertidas() throws Exception {
        enviar(HttpMethod.GET, "/api/movimientos?desde=" + haceHoras(1) + "&hasta=" + haceHoras(48),
                tokenTendero(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.accion").value("Invierta las fechas del filtro."));
    }

    @Test
    @DisplayName("RN-15: un rango de mas de 366 dias responde 409")
    void historialConRangoExcesivo() throws Exception {
        enviar(HttpMethod.GET, "/api/movimientos?desde=" + haceHoras(24 * 400) + "&hasta=" + haceHoras(0),
                tokenTendero(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").value("Consulte un periodo mas corto."));
    }
}
