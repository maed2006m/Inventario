package co.edu.javeriana.inventario;

import co.edu.javeriana.inventario.model.Rol;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la autenticacion (HU01, RN-12).
 *
 * Aqui si se pasa por /api/auth/login, porque es justamente lo que se
 * prueba: la contrasena se compara contra el hash BCrypt guardado en
 * PostgreSQL por la migracion V2 y el token devuelto debe servir para
 * las siguientes peticiones.
 */
class AuthApiIT extends ApiIntegracionBase {

    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    private static Map<String, String> credenciales(String usuario, String clave) {
        return Map.of("username", usuario, "password", clave);
    }

    @Test
    @DisplayName("HU01: login correcto devuelve un token que sirve para consultar /me")
    void loginCorrectoYConsultaDeMe() throws Exception {
        var login = enviar(HttpMethod.POST, "/api/auth/login", null, credenciales("admin", "Admin123*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nombre").value("Administrador"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.expiraEnMinutos").value(120));

        String token = leer(login).get("token").asText();

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("HU01: contrasena incorrecta responde 401 con accion correctiva")
    void contrasenaIncorrectaResponde401() throws Exception {
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales("tendero", "otra-clave"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.accion").isNotEmpty());
    }

    /**
     * Usuario inexistente y contrasena incorrecta deben responder lo
     * mismo, para no revelar que usuarios existen en el sistema.
     */
    @Test
    @DisplayName("HU01: usuario inexistente responde el mismo 401 que una contrasena incorrecta")
    void usuarioInexistenteResponde401() throws Exception {
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales("no-existe-" + sufijo(), "Admin123*"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accion").value("Verifique su usuario y contrasena."));
    }

    @Test
    @DisplayName("HU01: un usuario inactivo no puede iniciar sesion aunque la contrasena sea correcta")
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        Usuario inactivo = new Usuario();
        inactivo.setUsername("inactivo-" + sufijo());
        inactivo.setPasswordHash(encoder.encode("Clave123*"));
        inactivo.setNombre("Usuario inactivo");
        inactivo.setRol(Rol.TENDERO);
        inactivo.setActivo(false);
        assertNotNull(usuarios.save(inactivo).getCreadoEn(), "La fecha de creacion se asigna al persistir");

        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales(inactivo.getUsername(), "Clave123*"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HU01: usuario y contrasena vacios responden 400 con los campos senalados")
    void camposVaciosResponden400() throws Exception {
        enviar(HttpMethod.POST, "/api/auth/login", null, credenciales("", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.username").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    @DisplayName("RN-12: un token alterado se trata como anonimo y responde 401")
    void tokenAlteradoResponde401() throws Exception {
        String alterado = tokenTendero() + "x";
        mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, alterado))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer no.es.un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN-12: una cabecera Authorization que no es Bearer se ignora y responde 401")
    void cabeceraSinBearerResponde401() throws Exception {
        mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46QWRtaW4xMjMq"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Un token firmado correctamente para un usuario que ya no existe
     * (por ejemplo, eliminado despues de iniciar sesion) no debe dar
     * acceso a sus datos.
     */
    @Test
    @DisplayName("HU01: /me con un token de un usuario que no existe responde 401")
    void meConUsuarioInexistenteResponde401() throws Exception {
        String token = "Bearer " + jwt.generar("fantasma-" + sufijo(), "TENDERO");
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
