package co.edu.javeriana.inventario.config;

import co.edu.javeriana.inventario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrasenas de los usuarios iniciales en produccion (RNF-09).
 *
 * V2__usuarios_iniciales.sql crea admin y tendero con contrasenas de
 * demostracion que estan publicadas en el README. No se puede cambiar
 * esa migracion (Flyway rechaza migraciones ya aplicadas), y poner los
 * hashes de produccion en una migracion nueva los dejaria en Git.
 *
 * Por eso, solo con el perfil prod, al arrancar se toman las contrasenas
 * de las variables ADMIN_PASSWORD y TENDERO_PASSWORD y se guarda su hash
 * BCrypt. Si la contrasena guardada ya coincide no se escribe nada, asi
 * que reiniciar la aplicacion no cambia la base.
 */
@Slf4j
@Component
@Profile("prod")
public class CredencialesProduccion implements ApplicationRunner {

    static final int LONGITUD_MINIMA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String passwordAdmin;
    private final String passwordTendero;

    public CredencialesProduccion(UsuarioRepository usuarios,
                                  PasswordEncoder encoder,
                                  @Value("${app.usuarios.admin-password}") String passwordAdmin,
                                  @Value("${app.usuarios.tendero-password}") String passwordTendero) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.passwordAdmin = passwordAdmin;
        this.passwordTendero = passwordTendero;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        aplicar("admin", passwordAdmin);
        aplicar("tendero", passwordTendero);
    }

    void aplicar(String username, String password) {
        if (password == null || password.isBlank() || password.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException("La contrasena de produccion de '" + username
                    + "' debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        usuarios.findByUsername(username).ifPresentOrElse(usuario -> {
            if (!encoder.matches(password, usuario.getPasswordHash())) {
                usuario.setPasswordHash(encoder.encode(password));
                usuarios.save(usuario);
                log.info("Contrasena de produccion aplicada al usuario '{}'.", username);
            }
        }, () -> log.warn("El usuario '{}' no existe; no se cambio su contrasena.", username));
    }
}
