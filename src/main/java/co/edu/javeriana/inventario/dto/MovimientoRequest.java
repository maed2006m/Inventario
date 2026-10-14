package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos que el cliente envía para registrar un movimiento de inventario (RF-14 a RF-18).
 *
 * No incluye fecha ni usuario: la fecha la pone el sistema y el usuario sale del token de sesión.
 * El valor unitario es opcional; si no se envía, el servicio usa el costo del producto
 * (entradas, bajas) o su precio de venta (ventas), según RN-13.
 * La coherencia entre tipo y motivo se valida en el servicio (RN-04).
 */
public record MovimientoRequest(

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El tipo de movimiento es obligatorio (ENTRADA o SALIDA)")
        TipoMovimiento tipo,

        @NotNull(message = "El motivo es obligatorio (COMPRA, VENTA, AVERIA o VENCIMIENTO)")
        MotivoMovimiento motivo,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que 0")
        Integer cantidad,

        @PositiveOrZero(message = "El valor unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El valor admite hasta 10 enteros y 2 decimales")
        BigDecimal valorUnitario,

        @Size(max = 250, message = "La observación no puede superar 250 caracteres")
        String observacion) {
}