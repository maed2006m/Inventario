package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProductoResponse;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.service.ProductoService;
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
 * Endpoints de productos (HU4, HU5, HU6, HU10, HU11).
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @Operation(summary = "Crea un producto asociado a un proveedor activo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "El proveedor no existe"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado o proveedor inactivo")
    })
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    /**
     * Listado del catalogo con existencia y estado calculados.
     *
     * Los parametros opcionales se combinan: todos los que vengan se
     * aplican a la vez (RF-24).
     */
    @Operation(summary = "Lista productos con su existencia y estado de stock")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) EstadoStock estado,
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        return ResponseEntity.ok(service.listar(q, proveedorId, categoria, estado, soloActivos));
    }

    /** HU11: productos en BAJO_STOCK o AGOTADO. */
    @Operation(summary = "Lista los productos por debajo de su stock minimo")
    @GetMapping("/alertas")
    public ResponseEntity<List<ProductoResponse>> alertas() {
        return ResponseEntity.ok(service.alertas());
    }

    @Operation(summary = "Consulta un producto por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @Operation(summary = "Edita un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "No existe"),
            @ApiResponse(responseCode = "409", description = "SKU duplicado, SKU con movimientos o proveedor inactivo")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    /** RF-10, RF-11: deshabilitar conserva los movimientos. */
    @Operation(summary = "Deshabilita un producto del catalogo, conservando su historial")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto deshabilitado"),
            @ApiResponse(responseCode = "404", description = "No existe")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deshabilitar(@PathVariable Long id) {
        service.deshabilitar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vuelve a habilitar un producto deshabilitado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto habilitado"),
            @ApiResponse(responseCode = "404", description = "No existe"),
            @ApiResponse(responseCode = "409", description = "El proveedor esta inactivo")
    })
    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<ProductoResponse> habilitar(@PathVariable Long id) {
        return ResponseEntity.ok(service.habilitar(id));
    }
}
