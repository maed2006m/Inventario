package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.LoginRequest;
import co.edu.javeriana.inventario.dto.LoginResponse;
import co.edu.javeriana.inventario.dto.UsuarioResponse;
import co.edu.javeriana.inventario.exception.CredencialesInvalidasException;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import co.edu.javeriana.inventario.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Autenticacion (RNF-09, RNF-10).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    /**
     * POST /api/auth/login
     *
     * Las tres condiciones (existe, esta activo, la contrasena coincide)
     * fallan con el MISMO error a proposito, para no revelar que
     * usuarios existen en el sistema.
     */
    @Operation(summary = "Inicia sesion y devuelve un token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto"),
            @ApiResponse(responseCode = "400", description = "Faltan campos obligatorios"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {

        Usuario usuario = usuarios.findByUsername(req.username())
                .filter(Usuario::isActivo)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        String token = jwt.generar(usuario.getUsername(), usuario.getRol().name());

        return new LoginResponse(
                token,
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getRol().name(),
                jwt.getMinutos()
        );
    }

    /**
     * GET /api/auth/me
     *
     * Spring inyecta el Authentication desde el SecurityContextHolder
     * que lleno JwtAuthFilter. auth.getName() es el username del token,
     * asi que el cliente no puede falsear de quien se trata.
     */
    @Operation(summary = "Devuelve los datos del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token vencido")
    })
    @GetMapping("/me")
    public UsuarioResponse me(Authentication auth) {

        Usuario usuario = usuarios.findByUsername(auth.getName())
                .orElseThrow(CredencialesInvalidasException::new);

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getRol().name()
        );
    }
}
