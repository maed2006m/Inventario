package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.UsuarioRequest;
import co.edu.javeriana.inventario.dto.UsuarioResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.Rol;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas de la gestion de cuentas (RF-30).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock PasswordEncoder encoder;

    @InjectMocks UsuarioService service;

    private Usuario usuario(long id, String username, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setNombre("Nombre " + username);
        u.setRol(rol);
        u.setActivo(activo);
        u.setPasswordHash("hash");
        return u;
    }

    @Test
    @DisplayName("RF-30: crear guarda el usuario en minusculas y la contrasena solo como hash")
    void crearNormalizaUsuarioYCifraContrasena() {
        when(usuarios.existsByUsernameIgnoreCase("miguel")).thenReturn(false);
        when(encoder.encode("Clave123*")).thenReturn("$2a$10$hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse r = service.crear(new UsuarioRequest(" Miguel ", " Miguel Pérez ", "Clave123*", Rol.TENDERO));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertEquals("miguel", guardado.getValue().getUsername());
        assertEquals("Miguel Pérez", guardado.getValue().getNombre());
        assertEquals("$2a$10$hash", guardado.getValue().getPasswordHash());
        assertTrue(guardado.getValue().isActivo());
        assertEquals("TENDERO", r.rol());
        assertTrue(r.activo());
    }

    @Test
    @DisplayName("RF-30: el ADMIN tambien puede crear otro ADMIN (socio de la misma tienda)")
    void crearAdmin() {
        when(usuarios.existsByUsernameIgnoreCase("socio")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse r = service.crear(new UsuarioRequest("socio", "Socio", "Clave123*", Rol.ADMIN));

        assertEquals("ADMIN", r.rol());
    }

    @Test
    @DisplayName("RF-30: un usuario repetido se rechaza con accion correctiva y no se guarda")
    void crearUsuarioRepetidoSeRechaza() {
        when(usuarios.existsByUsernameIgnoreCase("tendero")).thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.crear(new UsuarioRequest("Tendero", "Otro", "Clave123*", Rol.TENDERO)));

        assertTrue(ex.getMessage().contains("tendero"));
        assertNotNull(ex.getAccion());
        verify(usuarios, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("RF-30: listar devuelve los datos publicos sin el hash")
    void listar() {
        when(usuarios.findAllByOrderByRolAscNombreAsc())
                .thenReturn(List.of(usuario(1, "admin", Rol.ADMIN, true), usuario(2, "tendero", Rol.TENDERO, false)));

        List<UsuarioResponse> lista = service.listar();

        assertEquals(2, lista.size());
        assertEquals("admin", lista.get(0).username());
        assertFalse(lista.get(1).activo());
    }

    @Test
    @DisplayName("RF-30: desactivar marca activo en false, no borra la cuenta")
    void desactivar() {
        Usuario u = usuario(2, "tendero", Rol.TENDERO, true);
        when(usuarios.findById(2L)).thenReturn(Optional.of(u));
        when(usuarios.save(u)).thenReturn(u);

        UsuarioResponse r = service.desactivar(2L, "admin");

        assertFalse(r.activo());
        verify(usuarios, never()).delete(any());
    }

    @Test
    @DisplayName("RF-30: nadie puede desactivar su propia cuenta")
    void noPuedeDesactivarseASiMismo() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1, "admin", Rol.ADMIN, true)));

        var ex = assertThrows(BusinessRuleException.class, () -> service.desactivar(1L, "ADMIN"));

        assertNotNull(ex.getAccion());
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("RF-30: activar vuelve a habilitar una cuenta desactivada")
    void activar() {
        Usuario u = usuario(2, "tendero", Rol.TENDERO, false);
        when(usuarios.findById(2L)).thenReturn(Optional.of(u));
        when(usuarios.save(u)).thenReturn(u);

        assertTrue(service.activar(2L).activo());
    }

    @Test
    @DisplayName("RF-30: activar o desactivar una cuenta inexistente responde 404")
    void cuentaInexistente() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.activar(99L));
        assertThrows(ResourceNotFoundException.class, () -> service.desactivar(99L, "admin"));
    }
}
