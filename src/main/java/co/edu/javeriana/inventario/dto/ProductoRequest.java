package co.edu.javeriana.inventario.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos que el cliente envía para crear o editar un producto (RF-06, RF-09).
 *
 * No incluye existencia (RN-01: se calcula, nunca se digita), ni id, ni estado activo.
 * El proveedor se indica solo por su id; el servicio valida que exista y esté activo (RN-06).
 */
public record ProductoRequest(

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 30, message = "El SKU no puede superar 30 caracteres")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 60, message = "La categoría no puede superar 60 caracteres")
        String categoria,

        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "El costo unitario es obligatorio")
        @PositiveOrZero(message = "El costo unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales")
        BigDecimal costoUnitario,

        @NotNull(message = "El precio de venta es obligatorio")
        @PositiveOrZero(message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precioVenta) {
}