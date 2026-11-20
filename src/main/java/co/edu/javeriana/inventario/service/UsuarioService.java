package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.UsuarioRequest;
import co.edu.javeriana.inventario.dto.UsuarioResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Gestion de cuentas (RF-30). Solo el ADMIN llega aqui (SecurityConfig).
 *
 * Todas las cuentas pertenecen a la misma tienda: un ADMIN nuevo es un
 * socio del mismo negocio, no otra tienda.
 *
 * Los usuarios no se borran: sus movimientos quedan en el historial con
 * su nombre (RNF-04), asi que "eliminar" es desactivar, igual que con
 * los proveedores (RN-07).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByRolAscNombreAsc().stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {

        String username = req.username().trim().toLowerCase(Locale.ROOT);

        if (usuarios.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException(
                    "Ya existe una cuenta con el usuario '" + username + "'.",
                    "Elija otro nombre de usuario o reactive la cuenta existente.");
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombre(req.nombre().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRol(req.rol());
        u.setActivo(true);

        return UsuarioResponse.de(usuarios.save(u));
    }

    /**
     * Una cuenta desactivada ya no puede iniciar sesion (AuthController
     * filtra por activo). Un token emitido antes sigue valido hasta que
     * vence (app.jwt.expiracion-minutos), porque la API es stateless.
     *
     * Nadie puede desactivar su propia cuenta: asi la tienda nunca se
     * queda sin al menos un ADMIN activo que pueda volver a habilitar a
     * los demas.
     */
    @Transactional
    public UsuarioResponse desactivar(Long id, String solicitante) {

        Usuario u = buscarOFallar(id);

        if (u.getUsername().equalsIgnoreCase(solicitante)) {
            throw new BusinessRuleException(
                    "No puede desactivar su propia cuenta.",
                    "Pida a otro administrador que lo haga si de verdad lo necesita.");
        }

        u.setActivo(false);
        return UsuarioResponse.de(usuarios.save(u));
    }

    @Transactional
    public UsuarioResponse activar(Long id) {
        Usuario u = buscarOFallar(id);
        u.setActivo(true);
        return UsuarioResponse.de(usuarios.save(u));
    }

    private Usuario buscarOFallar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + id + " no existe."));
    }
}
