package co.edu.javeriana.inventario.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Datos para crear o editar un producto (HU4, HU5).
 *
 * El proveedor se envia como proveedorId (un numero plano), no como
 * un objeto anidado. Asi el cliente no necesita conocer la estructura
 * interna de la entidad.
 *
 * NO existe un campo de existencia: RN-01 queda garantizada por el
 * diseno de la API, no por una validacion que alguien pueda olvidar.
 */
public record ProductoRequest(

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 30, message = "El SKU no puede superar 30 caracteres")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 60, message = "La categoria no puede superar 60 caracteres")
        String categoria,

        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @NotNull(message = "El stock minimo es obligatorio")
        @PositiveOrZero(message = "El stock minimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "El costo unitario es obligatorio")
        @PositiveOrZero(message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        @NotNull(message = "El precio de venta es obligatorio")
        @PositiveOrZero(message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta
) {
}
