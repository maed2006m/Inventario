
package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.dto.ProveedorResponse;
import co.edu.javeriana.inventario.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para administrar proveedores.
 *
 * Se encarga de recibir las solicitudes HTTP y delegar
 * las operaciones en ProveedorService.
 */
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    /**
     * Lista los proveedores.
     *
     * Por defecto, devuelve únicamente los proveedores activos.
     *
     * GET /api/proveedores
     * GET /api/proveedores?soloActivos=false
     */
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar(
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        List<ProveedorResponse> proveedores =
                proveedorService.listar(soloActivos);

        return ResponseEntity.ok(proveedores);
    }

    /**
     * Consulta un proveedor por su identificador.
     *
     * GET /api/proveedores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtener(
            @PathVariable Long id) {

        ProveedorResponse proveedor =
                proveedorService.obtener(id);

        return ResponseEntity.ok(proveedor);
    }

    /**
     * Registra un nuevo proveedor.
     *
     * POST /api/proveedores
     *
     * @Valid verifica las restricciones definidas en
     * ProveedorRequest.
     */
    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(
            @Valid @RequestBody ProveedorRequest request) {

        ProveedorResponse proveedor =
                proveedorService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(proveedor);
    }

    /**
     * Actualiza los datos de un proveedor existente.
     *
     * PUT /api/proveedores/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request) {

        ProveedorResponse proveedor =
                proveedorService.actualizar(id, request);

        return ResponseEntity.ok(proveedor);
    }

    /**
     * Inactiva un proveedor sin eliminarlo de la base de datos.
     *
     * PATCH /api/proveedores/{id}/inactivar
     *
     * El servicio impide la operación si el proveedor
     * todavía tiene productos activos.
     */
    @PatchMapping("/{id}/inactivar")
    public ResponseEntity<Void> inactivar(
            @PathVariable Long id) {

        proveedorService.inactivar(id);

        return ResponseEntity.noContent().build();
    }
}
