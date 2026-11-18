package co.edu.javeriana.inventario.config;

import co.edu.javeriana.inventario.model.Rol;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * En produccion las contrasenas de demostracion de V2 se reemplazan
 * por las de las variables de entorno (RNF-09).
 */
@ExtendWith(MockitoExtension.class)
class CredencialesProduccionTest {

    @Mock UsuarioRepository usuarios;

    // BCrypt con costo 4 para que la prueba sea rapida.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private CredencialesProduccion credenciales;

    @BeforeEach
    void crear() {
        credenciales = new CredencialesProduccion(usuarios, encoder, "ClaveAdmin2026!", "ClaveTendero2026!");
    }

    private Usuario usuario(String username, String password) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombre(username);
        u.setRol(Rol.ADMIN);
        u.setPasswordHash(encoder.encode(password));
        return u;
    }

    @Test
    @DisplayName("Reemplaza la contrasena de demostracion por la de produccion")
    void reemplazaContrasenaDemo() {
        Usuario admin = usuario("admin", "Admin123*");
        Usuario tendero = usuario("tendero", "Tendero123*");
        when(usuarios.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(usuarios.findByUsername("tendero")).thenReturn(Optional.of(tendero));

        credenciales.run(null);

        assertTrue(encoder.matches("ClaveAdmin2026!", admin.getPasswordHash()));
        assertFalse(encoder.matches("Admin123*", admin.getPasswordHash()));
        assertTrue(encoder.matches("ClaveTendero2026!", tendero.getPasswordHash()));
        verify(usuarios).save(admin);
        verify(usuarios).save(tendero);
    }

    @Test
    @DisplayName("Si la contrasena ya coincide no escribe en la base")
    void noReescribeSiYaCoincide() {
        Usuario admin = usuario("admin", "ClaveAdmin2026!");
        when(usuarios.findByUsername("admin")).thenReturn(Optional.of(admin));

        credenciales.aplicar("admin", "ClaveAdmin2026!");

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Si el usuario no existe no falla ni guarda nada")
    void usuarioInexistente() {
        when(usuarios.findByUsername("admin")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> credenciales.aplicar("admin", "ClaveAdmin2026!"));
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza contrasenas vacias o cortas para no arrancar con una clave debil")
    void rechazaContrasenaCorta() {
        assertThrows(IllegalStateException.class, () -> credenciales.aplicar("admin", "corta"));
        assertThrows(IllegalStateException.class, () -> credenciales.aplicar("admin", "   "));
        verifyNoInteractions(usuarios);
    }
}
