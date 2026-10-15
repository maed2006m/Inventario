package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.dto.ProveedorResponse;
import co.edu.javeriana.inventario.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de proveedores (HU1, HU2, HU3).
 *
 * PUT y DELETE estan restringidos a ADMIN en SecurityConfig, no aqui:
 * los permisos viven en un solo lugar para poder leerlos de un vistazo.
 *
 * Este controlador no contiene reglas de negocio, solo traduce HTTP.
 */
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    @Operation(summary = "Crea un proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "El NIT ya existe")
    })
    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Lista los proveedores")
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(service.listar(soloActivos));
    }

    @Operation(summary = "Consulta un proveedor por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Edita un proveedor (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "No existe"),
            @ApiResponse(responseCode = "409", description = "El NIT ya existe")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ProveedorRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    /**
     * Inactivacion logica (RF-03, RF-04, RN-07).
     *
     * El verbo es DELETE porque para el usuario la accion es "eliminar",
     * pero el efecto real es marcar activo = false y conservar el
     * historial. Responde 204 porque no hay nada que devolver.
     */
    @Operation(summary = "Inactiva un proveedor, conservando su historial (solo ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Proveedor inactivado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "No existe"),
            @ApiResponse(responseCode = "409", description = "Tiene productos activos")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inactivar(@PathVariable Long id) {
        service.inactivar(id);
        return ResponseEntity.noContent().build();
    }
}
