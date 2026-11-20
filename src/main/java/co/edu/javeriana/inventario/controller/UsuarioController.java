package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.UsuarioRequest;
import co.edu.javeriana.inventario.dto.UsuarioResponse;
import co.edu.javeriana.inventario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion de cuentas (RF-30).
 *
 * Todo /api/usuarios/** es solo ADMIN; la regla esta en SecurityConfig,
 * junto con el resto de la matriz de permisos.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @Operation(summary = "Lista las cuentas de la tienda (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN")
    })
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @Operation(summary = "Crea una cuenta de tendero o administrador (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "El usuario ya existe")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Desactiva una cuenta; ya no podra iniciar sesion (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta desactivada"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "No existe"),
            @ApiResponse(responseCode = "409", description = "Es la cuenta de quien hace la solicitud")
    })
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.desactivar(id, auth.getName()));
    }

    @Operation(summary = "Reactiva una cuenta (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta activada"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @PutMapping("/{id}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(service.activar(id));
    }
}
