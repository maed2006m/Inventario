package co.edu.javeriana.inventario;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.security.JwtService;
import co.edu.javeriana.inventario.service.MovimientoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la API de movimientos.
 *
 * A diferencia de las unitarias, aqui NO hay mocks: la peticion
 * atraviesa el filtro JWT, el controlador, el servicio, Hibernate y
 * llega a un PostgreSQL real con sus triggers activos.
 */
class MovimientoApiIT extends IntegracionBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired MovimientoService movimientoService;

    // ---------- utilidades ----------

    private String tokenTendero() {
        return "Bearer " + jwt.generar("tendero", "TENDERO");
    }

    private String tokenAdmin() {
        return "Bearer " + jwt.generar("admin", "ADMIN");
    }

    private ResultActions enviar(HttpMethod metodo, String ruta, String token, Object cuerpo) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, ruta)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (cuerpo != null) {
            req.content(json.writeValueAsString(cuerpo));
        }
        return mvc.perform(req);
    }

    private long leerId(ResultActions acciones) throws Exception {
        return json.readTree(acciones.andReturn().getResponse().getContentAsString())
                .get("id").asLong();
    }

    /**
     * Crea un proveedor y un producto nuevos para cada prueba.
     *
     * El sufijo basado en nanoTime evita chocar con el UNIQUE de nit
     * y sku entre pruebas, ya que comparten el mismo contenedor.
     */
    private long crearProductoNuevo() throws Exception {
String sufijo = String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L));

        var proveedor = enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(),
                Map.of("nombre", "Proveedor IT", "nit", sufijo, "tiempoEntregaDias", 2))
                .andExpect(status().isCreated());

        long proveedorId = leerId(proveedor);

        var producto = enviar(HttpMethod.POST, "/api/productos", tokenTendero(),
                Map.of("sku", "IT-" + sufijo, "nombre", "Producto IT",
                        "proveedorId", proveedorId, "stockMinimo", 3,
                        "costoUnitario", 1000, "precioVenta", 1500))
                .andExpect(status().isCreated());

        return leerId(producto);
    }

    // ---------- seguridad ----------

    @Test
    @DisplayName("RN-12: sin token, la API responde 401")
    void sinTokenResponde401() throws Exception {
        enviar(HttpMethod.GET, "/api/productos", null, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RNF-10: el tendero no puede editar proveedores ni ver reportes")
    void tenderoNoPuedeEditarProveedoresNiVerReportes() throws Exception {

        enviar(HttpMethod.PUT, "/api/proveedores/1", tokenTendero(),
                Map.of("nombre", "Cambio", "nit", "900999999-9", "tiempoEntregaDias", 2))
                .andExpect(status().isForbidden());

        enviar(HttpMethod.DELETE, "/api/proveedores/1", tokenTendero(), null)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RNF-10: el admin si puede editar proveedores")
    void adminSiPuedeEditarProveedores() throws Exception {

        long productoId = crearProductoNuevo();
        assertTrue(productoId > 0);

        // Un 403 significaria que la regla de rol esta mal; un 404 o
        // un 409 serian respuestas de negocio, no de permisos.
        enviar(HttpMethod.PUT, "/api/proveedores/999999", tokenAdmin(),
                Map.of("nombre", "X", "nit", "900888888-8", "tiempoEntregaDias", 2))
                .andExpect(status().isNotFound());
    }

    // ---------- flujo de negocio ----------

    /**
     * El recorrido completo: entrada, venta y rechazo.
     *
     * Es la evidencia de RF-14, RF-17, RF-20, RF-21 y RF-22 en una
     * sola prueba.
     */
    @Test
    @DisplayName("RF-20, RN-02: entrada, venta y rechazo por existencia insuficiente")
    void flujoEntradaVentaYRechazo() throws Exception {

        long id = crearProductoNuevo();

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "ENTRADA", "motivo", "COMPRA", "cantidad", 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.existenciaResultante").value(10));

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "SALIDA", "motivo", "VENTA", "cantidad", 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.existenciaResultante").value(6));

        // La salida que no cabe: 409 con causa Y accion correctiva (RNF-03)
        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "SALIDA", "motivo", "VENTA", "cantidad", 7))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.accion").isNotEmpty());

        // La existencia no cambio por el intento rechazado
        enviar(HttpMethod.GET, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencia").value(6))
                .andExpect(jsonPath("$.estado").value("EN_STOCK"));
    }

    @Test
    @DisplayName("RN-03: una cantidad negativa responde 400 con el campo senalado")
    void cantidadNegativaResponde400() throws Exception {

        long id = crearProductoNuevo();

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "ENTRADA", "motivo", "COMPRA", "cantidad", -5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").exists());
    }

    @Test
    @DisplayName("RN-10: el estado pasa a BAJO_STOCK al cruzar el minimo")
    void estadoCambiaABajoStock() throws Exception {

        long id = crearProductoNuevo();   // stockMinimo = 3

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "ENTRADA", "motivo", "COMPRA", "cantidad", 10))
                .andExpect(status().isCreated());

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "SALIDA", "motivo", "VENTA", "cantidad", 8))
                .andExpect(status().isCreated());

        enviar(HttpMethod.GET, "/api/productos/" + id, tokenTendero(), null)
                .andExpect(jsonPath("$.existencia").value(2))
                .andExpect(jsonPath("$.estado").value("BAJO_STOCK"));
    }

    // ---------- inmutabilidad ----------

    @Test
    @DisplayName("RN-05: no existen los metodos PUT ni DELETE sobre movimientos")
    void movimientosNoSePuedenEditarPorLaApi() throws Exception {

        enviar(HttpMethod.PUT, "/api/movimientos/1", tokenTendero(), Map.of("cantidad", 99))
                .andExpect(status().isMethodNotAllowed());

        enviar(HttpMethod.DELETE, "/api/movimientos/1", tokenTendero(), null)
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * RNF-04: la inmutabilidad tambien se defiende en la base.
     *
     * Esta prueba va directo con JdbcTemplate, saltandose toda la
     * aplicacion, para demostrar que ni siquiera una consulta manual
     * puede alterar el historial. Es el trigger trg_movimientos_inmutables.
     */
    @Test
    @DisplayName("RNF-04: la base bloquea UPDATE y DELETE sobre movimientos")
    void movimientosSonInmutablesEnLaBase() throws Exception {

        long id = crearProductoNuevo();

        enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", id, "tipo", "ENTRADA", "motivo", "COMPRA", "cantidad", 5))
                .andExpect(status().isCreated());

        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE movimientos SET cantidad = 99 WHERE producto_id = ?", id));

        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM movimientos WHERE producto_id = ?", id));
    }

    // ---------- concurrencia ----------

    /**
     * RN-14, RNF-05: LA PRUEBA MAS IMPORTANTE DEL PROYECTO.
     *
     * Lanza dos ventas de 6 unidades al mismo tiempo sobre un stock
     * de 10. Solo una puede ganar: 6 + 6 = 12 dejaria el inventario
     * en -2.
     *
     * Sin el bloqueo pesimista de findByIdForUpdate, ambos hilos
     * leerian existencia 10, ambos pasarian la validacion de RN-02 y
     * ambos insertarian. Con el bloqueo, el segundo espera a que el
     * primero termine y entonces lee la existencia real, que ya es 4.
     *
     * El CountDownLatch es lo que hace que ambos hilos arranquen en
     * el mismo instante; sin el, uno terminaria antes de que el otro
     * empiece y la condicion de carrera nunca ocurriria.
     */
    @Test
    @DisplayName("RN-14: dos ventas simultaneas nunca dejan el stock negativo")
    void dosVentasSimultaneasNuncaDejanElStockNegativo() throws Exception {

        long id = crearProductoNuevo();

        movimientoService.registrar(new MovimientoRequest(
                id, TipoMovimiento.ENTRADA, MotivoMovimiento.COMPRA, 10, null, null), "tendero");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);

        Callable<Boolean> venta6 = () -> {
            largada.await();
            try {
                movimientoService.registrar(new MovimientoRequest(
                        id, TipoMovimiento.SALIDA, MotivoMovimiento.VENTA, 6, null, null), "tendero");
                return true;
                        } catch (RuntimeException e) {
                return false;
            }
        };

        Future<Boolean> a = pool.submit(venta6);
        Future<Boolean> b = pool.submit(venta6);

        largada.countDown();   // ambos arrancan a la vez

        int exitosas = (a.get(30, TimeUnit.SECONDS) ? 1 : 0)
                     + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);

        assertEquals(1, exitosas, "Exactamente una venta debe prosperar");

        Integer existencia = jdbc.queryForObject(
                "SELECT existencia FROM v_stock_producto WHERE producto_id = ?",
                Integer.class, id);

        assertEquals(4, existencia, "10 - 6 = 4; nunca -2");

        pool.shutdown();
    }
}
