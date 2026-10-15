package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Movimiento registrado.
 *
 * existenciaResultante es el saldo del producto despues de aplicar
 * este movimiento. Se incluye para que el tendero vea de inmediato
 * como quedo el inventario, sin tener que consultar aparte.
 */
public record MovimientoResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String proveedorNombre,
        TipoMovimiento tipo,
        MotivoMovimiento motivo,
        Integer cantidad,
        BigDecimal valorUnitario,
        String observacion,
        String usuario,
        OffsetDateTime fecha,
        Integer existenciaResultante
) {
}
