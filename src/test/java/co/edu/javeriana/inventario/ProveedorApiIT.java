package co.edu.javeriana.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de proveedores (HU01 a HU03, RN-07, RN-09).
 *
 * Editar e inactivar son exclusivas del ADMIN (RNF-10); los permisos ya
 * los cubre MovimientoApiIT, aqui se prueban las reglas de negocio.
 */
class ProveedorApiIT extends ApiIntegracionBase {

    private boolean listadoContiene(String ruta, long id) throws Exception {
        JsonNode lista = leer(enviar(HttpMethod.GET, ruta, tokenTendero(), null).andExpect(status().isOk()));
        for (JsonNode p : lista) {
            if (p.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("HU01: consultar un proveedor devuelve sus datos y cuantos productos activos tiene")
    void obtenerProveedor() throws Exception {
        long id = crearProveedor();
        crearProducto(id, "PV-" + sufijo(), null, 1);
        crearProducto(id, "PV-" + sufijo(), null, 1);

        enviar(HttpMethod.GET, "/api/proveedores/" + id, tokenTendero(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Proveedor IT"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.productosActivos").value(2));
    }

    @Test
    @DisplayName("HU01: consultar un proveedor inexistente responde 404")
    void obtenerProveedorInexistente() throws Exception {
        enviar(HttpMethod.GET, "/api/proveedores/" + ID_INEXISTENTE, tokenTendero(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RN-07: un proveedor inactivo sale del listado de activos pero sigue en el completo")
    void listadoConInactivos() throws Exception {
        long id = crearProveedor();
        inactivarProveedor(id);

        assertFalse(listadoContiene("/api/proveedores?soloActivos=true", id));
        assertTrue(listadoContiene("/api/proveedores", id));

        enviar(HttpMethod.GET, "/api/proveedores/" + id, tokenTendero(), null)
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("RN-09: crear un proveedor con un NIT repetido responde 409")
    void crearConNitRepetido() throws Exception {
        String nit = sufijo();
        enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(), proveedor("Original", nit))
                .andExpect(status().isCreated());

        enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(), proveedor("Copia", nit))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RN-09: un NIT con formato invalido responde 400 con el campo senalado")
    void crearConNitInvalido() throws Exception {
        enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(), proveedor("Proveedor", "12-AB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nit").exists());
    }

    @Test
    @DisplayName("HU02: el admin edita los datos de contacto de un proveedor")
    void editarProveedor() throws Exception {
        long id = crearProveedor();
        String nit = sufijo();
        Map<String, Object> cambios = new HashMap<>(proveedor("Proveedor editado", nit));
        cambios.put("telefono", "3001234567");
        cambios.put("email", "ventas@proveedor.co");
        cambios.put("direccion", "Calle 1 # 2-3");

        enviar(HttpMethod.PUT, "/api/proveedores/" + id, tokenAdmin(), cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Proveedor editado"))
                .andExpect(jsonPath("$.nit").value(nit))
                .andExpect(jsonPath("$.telefono").value("3001234567"))
                .andExpect(jsonPath("$.email").value("ventas@proveedor.co"))
                .andExpect(jsonPath("$.productosActivos").value(0));
    }

    @Test
    @DisplayName("RN-09: editar un proveedor con el NIT de otro responde 409")
    void editarConNitDeOtro() throws Exception {
        String nitOcupado = sufijo();
        enviar(HttpMethod.POST, "/api/proveedores", tokenTendero(), proveedor("Ocupa NIT", nitOcupado))
                .andExpect(status().isCreated());
        long id = crearProveedor();

        enviar(HttpMethod.PUT, "/api/proveedores/" + id, tokenAdmin(), proveedor("Cambio", nitOcupado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RN-07: no se puede inactivar un proveedor con productos activos")
    void inactivarConProductosActivos() throws Exception {
        long id = crearProveedor();
        crearProducto(id, "ACT-" + sufijo(), null, 1);

        enviar(HttpMethod.DELETE, "/api/proveedores/" + id, tokenAdmin(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1 producto")))
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("HU03: inactivar un proveedor inexistente responde 404")
    void inactivarInexistente() throws Exception {
        enviar(HttpMethod.DELETE, "/api/proveedores/" + ID_INEXISTENTE, tokenAdmin(), null)
                .andExpect(status().isNotFound());
    }
}
