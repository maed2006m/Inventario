
package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.service.MovimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para registrar movimientos de inventario.
 */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    /**
     * Registra una entrada o una salida del inventario.
     *
     * POST /api/movimientos
     *
     * El usuario se obtiene de la autenticación,
     * no del cuerpo de la petición.
     */
    @PostMapping
    public ResponseEntity<MovimientoResponse> registrar(
            @Valid @RequestBody MovimientoRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        MovimientoResponse respuesta =
                movimientoService.registrar(request, username);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
