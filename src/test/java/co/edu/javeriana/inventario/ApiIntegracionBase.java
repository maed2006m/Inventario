package co.edu.javeriana.inventario;

import co.edu.javeriana.inventario.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Utilidades compartidas por las pruebas de integracion que recorren la API.
 *
 * Cada prueba crea sus propios proveedores, productos y movimientos con un
 * sufijo aleatorio: todas las clases comparten el mismo contenedor y el orden
 * de ejecucion no esta garantizado, asi que ninguna prueba puede suponer que
 * la base esta vacia ni depender de datos creados por otra.
 */
abstract class ApiIntegracionBase extends IntegracionBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JwtService jwt;

    /** Id que ninguna secuencia de la base alcanzara durante las pruebas. */
    protected static final long ID_INEXISTENTE = 999_999_999L;

    protected String tokenTendero() {
        return "Bearer " + jwt.generar("tendero", "TENDERO");
    }

    protected String tokenAdmin() {
        return "Bearer " + jwt.generar("admin", "ADMIN");
    }

    protected ResultActions enviar(HttpMethod metodo, String ruta, String token, Object cuerpo) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, ruta).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, token);
        }
        if (cuerpo != null) {
            req.content(json.writeValueAsString(cuerpo));
        }
        return mvc.perform(req);
    }

    protected JsonNode leer(ResultActions acciones) throws Exception {
        return json.readTree(acciones.andReturn().getResponse().getContentAsString());
    }

    protected long leerId(ResultActions acciones) throws Exception {
        return leer(acciones).get("id").asLong();
    }

    /** Nueve digitos: sirve como NIT valido y como parte unica de un SKU. */
    protected static String sufijo() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L));
    }

    protected static Map<String, Object> proveedor(String nombre, String nit) {
        return Map.of("nombre", nombre, "nit", nit, "tiempoEntregaDias", 2);
    }

    protected static Map<String, Object> producto(String sku, String nombre, String categoria,
                                                  long proveedorId, int stockMinimo) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("sku", sku);
        cuerpo.put("nombre", nombre);
        cuerpo.put("categoria", categoria);
        cuerpo.put("proveedorId", proveedorId);
        cuerpo.put("stockMinimo", stockMinimo);
        cuerpo.put("costoUnitario", 1000);
        cuerpo.put("precioVenta", 1500);
        return cuerpo;
    }

    protected long crearProveedor() throws Exception {
        return leerId(enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(),
                proveedor("Proveedor IT", sufijo()))
                .andExpect(status().isCreated()));
    }

    protected long crearProducto(long proveedorId, String sku, String categoria, int stockMinimo) throws Exception {
        return leerId(enviar(HttpMethod.POST, "/api/productos", tokenTendero(),
                producto(sku, "Producto " + sku, categoria, proveedorId, stockMinimo))
                .andExpect(status().isCreated()));
    }

    /** Proveedor nuevo con un producto nuevo (stock minimo 3). */
    protected long crearProducto() throws Exception {
        return crearProducto(crearProveedor(), "IT-" + sufijo(), null, 3);
    }

    protected ResultActions registrar(long productoId, String tipo, String motivo, int cantidad) throws Exception {
        return enviar(HttpMethod.POST, "/api/movimientos", tokenTendero(),
                Map.of("productoId", productoId, "tipo", tipo, "motivo", motivo, "cantidad", cantidad));
    }

    protected void inactivarProveedor(long proveedorId) throws Exception {
        enviar(HttpMethod.DELETE, "/api/proveedores/" + proveedorId, tokenAdmin(), null)
                .andExpect(status().isNoContent());
    }
}
