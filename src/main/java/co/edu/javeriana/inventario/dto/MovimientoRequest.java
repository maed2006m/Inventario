package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Registro de una entrada o salida (HU7, HU8, HU9).
 *
 * No lleva usuarioId: el sistema lo toma del token para que nadie
 * pueda registrar un movimiento a nombre de otro.
 *
 * valorUnitario es opcional (RN-13): si no viene, la entrada usa el
 * costo del producto y la venta su precio de venta.
 */
public record MovimientoRequest(

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "El motivo es obligatorio")
        MotivoMovimiento motivo,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que 0")
        Integer cantidad,

        @PositiveOrZero(message = "El valor unitario no puede ser negativo")
        BigDecimal valorUnitario,

        @Size(max = 250, message = "La observacion no puede superar 250 caracteres")
        String observacion
) {
}
