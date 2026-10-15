package co.edu.javeriana.inventario.dto;

import jakarta.validation.constraints.*;

/**
 * Datos para crear o editar un proveedor (HU1, HU2).
 *
 * No incluye "activo": el estado se cambia solo por el endpoint de
 * inactivacion, no editando el proveedor. Asi la eliminacion logica
 * queda como una operacion explicita y auditable.
 */
public record ProveedorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "\\d{6,12}-?\\d?", message = "El NIT debe tener entre 6 y 12 digitos, con digito de verificacion opcional")
        String nit,

        @Pattern(regexp = "\\d{7,15}|", message = "El telefono debe tener entre 7 y 15 digitos")
        String telefono,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 120)
        String email,

        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        String direccion,

        @NotNull(message = "El tiempo de entrega es obligatorio")
        @PositiveOrZero(message = "El tiempo de entrega no puede ser negativo")
        Integer tiempoEntregaDias
) {
}
