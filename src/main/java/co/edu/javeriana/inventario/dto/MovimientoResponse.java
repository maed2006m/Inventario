package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Datos de un movimiento que la API devuelve al cliente (RF-14, RF-27, RF-28).
 *
 * existenciaResultante es la existencia del producto justo después de registrar el movimiento.
 * Solo se calcula al registrar; en el historial no se envía (queda fuera del JSON).
 * Del producto, el proveedor y el usuario viajan solo los nombres, no las entidades.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        Integer existenciaResultante) {
}