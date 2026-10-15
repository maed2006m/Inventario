
package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProductoResponse;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de productos.
 *
 * Expone operaciones para listar, consultar, crear, actualizar,
 * deshabilitar y habilitar productos, además de consultar alertas
 * de inventario.
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Lista productos aplicando filtros opcionales.
     *
     * Ejemplo:
     * GET /api/productos?q=arroz&soloActivos=true
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) EstadoStock estado,
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        List<ProductoResponse> respuesta = productoService.listar(
                q,
                proveedorId,
                categoria,
                estado,
                soloActivos);

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Consulta los productos agotados o con stock bajo.
     *
     * GET /api/productos/alertas
     */
    @GetMapping("/alertas")
    public ResponseEntity<List<ProductoResponse>> alertas() {
        return ResponseEntity.ok(productoService.alertas());
    }

    /**
     * Consulta un producto por su identificador.
     *
     * GET /api/productos/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(
            @PathVariable Long id) {

        return ResponseEntity.ok(productoService.obtener(id));
    }

    /**
     * Crea un producto.
     *
     * POST /api/productos
     */
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(
            @Valid @RequestBody ProductoRequest request) {

        ProductoResponse creado = productoService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creado);
    }

    /**
     * Actualiza los datos de un producto existente.
     *
     * PUT /api/productos/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {

        return ResponseEntity.ok(
                productoService.actualizar(id, request));
    }

    /**
     * Deshabilita un producto sin eliminar su historial.
     *
     * PATCH /api/productos/1/deshabilitar
     */
    @PatchMapping("/{id}/deshabilitar")
    public ResponseEntity<Void> deshabilitar(
            @PathVariable Long id) {

        productoService.deshabilitar(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Habilita nuevamente un producto.
     *
     * PATCH /api/productos/1/habilitar
     */
    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<ProductoResponse> habilitar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                productoService.habilitar(id));
    }
}
