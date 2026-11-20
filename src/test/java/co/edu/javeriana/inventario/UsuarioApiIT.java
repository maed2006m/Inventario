package co.edu.javeriana.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la gestion de cuentas (RF-30).
 *
 * Recorren el filtro JWT, la matriz de permisos, el controlador y la
 * base real, y comprueban que la cuenta creada puede iniciar sesion.
 */
class UsuarioApiIT extends ApiIntegracionBase {

    private static Map<String, Object> cuenta(String username, String rol) {
        Map<String, Object> c = new HashMap<>();
        c.put("username", username);
        c.put("nombre", "Cuenta " + username);
        c.put("password", "Clave123*");
        c.put("rol", rol);
        return c;
    }

    private static Map<String, String> credenciales(String usuario, String clave) {
        return Map.of("username", usuario, "password", clave);
    }

    private long crearCuenta(String username, String rol) throws Exception {
        return leerId(enviar(HttpMethod.POST, "/api/usuarios", tokenAdmin(), cuenta(username, rol))
                .andExpect(status().isCreated()));
    }

    @Test
    @DisplayName("RF-30: el ADMIN crea un tendero, que puede iniciar sesion con su contrasena")
    void adminCreaTenderoQuePuedeIniciarSesion() throws Exception {
        String username = "tend" + sufijo();

        enviar(HttpMethod.POST, "/api/usuarios", tokenAdmin(), cuenta(username.toUpperCase(), "TENDERO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("TENDERO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        // El login no distingue mayusculas en el usuario.
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales(username.toUpperCase(), "Clave123*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("TENDERO"));
    }

    @Test
    @DisplayName("RF-30: el ADMIN crea otro ADMIN y aparece en el listado")
    void adminCreaAdminYLoLista() throws Exception {
        String username = "socio" + sufijo();
        long id = crearCuenta(username, "ADMIN");

        JsonNode lista = leer(enviar(HttpMethod.GET, "/api/usuarios", tokenAdmin(), null)
                .andExpect(status().isOk()));
        boolean encontrado = false;
        for (JsonNode u : lista) {
            if (u.get("id").asLong() == id) {
                encontrado = "ADMIN".equals(u.get("rol").asText());
            }
        }
        assertTrue(encontrado, "La cuenta ADMIN creada debe aparecer en el listado");
    }

    @Test
    @DisplayName("RF-30: el tendero no puede listar ni crear cuentas (403)")
    void tenderoNoGestionaCuentas() throws Exception {
        enviar(HttpMethod.GET, "/api/usuarios", tokenTendero(), null)
                .andExpect(status().isForbidden());
        enviar(HttpMethod.POST, "/api/usuarios", tokenTendero(), cuenta("intruso" + sufijo(), "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-30: sin token responde 401")
    void sinTokenResponde401() throws Exception {
        enviar(HttpMethod.GET, "/api/usuarios", null, null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-30: un usuario repetido responde 409 con accion correctiva")
    void usuarioRepetidoResponde409() throws Exception {
        enviar(HttpMethod.POST, "/api/usuarios", tokenAdmin(), cuenta("Tendero", "TENDERO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RF-30: datos invalidos responden 400 senalando los campos")
    void datosInvalidosResponden400() throws Exception {
        Map<String, Object> mala = new HashMap<>();
        mala.put("username", "con espacio");
        mala.put("nombre", "");
        mala.put("password", "corta");
        mala.put("rol", null);

        enviar(HttpMethod.POST, "/api/usuarios", tokenAdmin(), mala)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.username").isNotEmpty())
                .andExpect(jsonPath("$.campos.nombre").isNotEmpty())
                .andExpect(jsonPath("$.campos.password").isNotEmpty())
                .andExpect(jsonPath("$.campos.rol").isNotEmpty());
    }

    @Test
    @DisplayName("RF-30: una cuenta desactivada no inicia sesion y al reactivarla vuelve a entrar")
    void desactivarYActivar() throws Exception {
        String username = "temp" + sufijo();
        long id = crearCuenta(username, "TENDERO");

        enviar(HttpMethod.PUT, "/api/usuarios/" + id + "/desactivar", tokenAdmin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales(username, "Clave123*"))
                .andExpect(status().isUnauthorized());

        enviar(HttpMethod.PUT, "/api/usuarios/" + id + "/activar", tokenAdmin(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales(username, "Clave123*"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF-30: el ADMIN no puede desactivar su propia cuenta (409)")
    void adminNoSeDesactivaASiMismo() throws Exception {
        long idAdmin = -1;
        for (JsonNode u : leer(enviar(HttpMethod.GET, "/api/usuarios", tokenAdmin(), null))) {
            if ("admin".equals(u.get("username").asText())) {
                idAdmin = u.get("id").asLong();
            }
        }

        enviar(HttpMethod.PUT, "/api/usuarios/" + idAdmin + "/desactivar", tokenAdmin(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    @Test
    @DisplayName("RF-30: activar una cuenta inexistente responde 404")
    void cuentaInexistenteResponde404() throws Exception {
        enviar(HttpMethod.PUT, "/api/usuarios/" + ID_INEXISTENTE + "/activar", tokenAdmin(), null)
                .andExpect(status().isNotFound());
    }
}
