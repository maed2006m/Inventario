package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.EstadoStock;

import java.math.BigDecimal;

/**
 * Producto tal como lo devuelve la API.
 *
 * existencia y estado se calculan al momento de responder; no se
 * guardan en ninguna tabla (RN-01, RN-10).
 */
public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String categoria,
        Long proveedorId,
        String proveedorNombre,
        Integer stockMinimo,
        BigDecimal costoUnitario,
        BigDecimal precioVenta,
        boolean activo,
        Integer existencia,
        EstadoStock estado
) {
}
