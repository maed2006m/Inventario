package co.edu.javeriana.inventario.dto;

import java.math.BigDecimal;

/**
 * Una fila del ranking de rotación de productos (RF-25, HU12).
 *
 * @param posicion         lugar en el ranking; 1 es el producto más vendido del periodo
 * @param productoId       identificador del producto
 * @param productoNombre   nombre del producto
 * @param proveedorNombre  proveedor que lo surte
 * @param unidadesVendidas unidades vendidas en el periodo (solo salidas por VENTA)
 * @param ingresos         dinero generado por esas ventas (cantidad por valor unitario)
 * @param indiceRotacion   unidades vendidas / existencia promedio del periodo;
 *                         es null si el producto no tuvo existencia en el periodo
 */
public record RotacionResponse(
        int posicion,
        Long productoId,
        String productoNombre,
        String proveedorNombre,
        long unidadesVendidas,
        BigDecimal ingresos,
        BigDecimal indiceRotacion) {
}