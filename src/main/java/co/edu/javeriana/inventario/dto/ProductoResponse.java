package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.EstadoStock;

import java.math.BigDecimal;

/**
 * Datos de un producto que la API devuelve al cliente (RF-12, RF-20, RF-23).
 *
 * existencia y estado no se guardan en la base: el servicio los calcula
 * a partir de los movimientos (RN-01) y del stock mínimo (RN-10).
 * Del proveedor solo viajan su id y su nombre.
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
        EstadoStock estado) {
}