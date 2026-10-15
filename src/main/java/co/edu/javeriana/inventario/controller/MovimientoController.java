package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.service.MovimientoConsultaService;
import co.edu.javeriana.inventario.service.MovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * Endpoints de movimientos (HU7, HU8, HU9, HU14).
 *
 * NO hay @PutMapping ni @DeleteMapping, y es deliberado: RN-05 y
 * RNF-04 establecen que los movimientos son inmutables. Al no existir
 * esos metodos, Spring responde 405 Method Not Allowed por si solo.
 *
 * Corregir un error de digitacion se hace registrando un movimiento
 * de ajuste que lo compense, no editando el original.
 */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService service;
    private final MovimientoConsultaService consulta;

    /**
     * Registra una entrada o una salida.
     *
     * Spring inyecta el Authentication desde el contexto que lleno
     * JwtAuthFilter. auth.getName() es el username del token, asi que
     * el cliente no puede registrar movimientos a nombre de otro.
     */
    @Operation(summary = "Registra una entrada o salida de inventario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimiento registrado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "El producto no existe"),
            @ApiResponse(responseCode = "409", description = "Existencia insuficiente, motivo incorrecto o producto deshabilitado")
    })
    @PostMapping
    public ResponseEntity<MovimientoResponse> registrar(@Valid @RequestBody MovimientoRequest req,
                                                        Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(req, auth.getName()));
    }

    /**
     * Historial filtrable y paginado (HU14, RF-27, RF-28).
     *
     * Spring construye el Pageable a partir de los parametros page,
     * size y sort de la URL. Por defecto trae 20 por pagina, de la
     * mas reciente a la mas antigua.
     */
    @Operation(summary = "Consulta el historial de movimientos con filtros combinables")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial paginado"),
            @ApiResponse(responseCode = "400", description = "Rango de fechas invalido")
    })
    @GetMapping
    public ResponseEntity<Page<MovimientoResponse>> historial(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) MotivoMovimiento motivo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                consulta.buscar(productoId, proveedorId, tipo, motivo, desde, hasta, pageable));
    }
}
