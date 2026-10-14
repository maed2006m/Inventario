package co.edu.javeriana.inventario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente envía para crear o editar un proveedor (RF-01, RF-02).
 * No incluye id ni estado activo: esos los controla el sistema (RN-07).
 */
public record ProveedorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotBlank(message = "El NIT es obligatorio")
        @Pattern(regexp = "\\d{6,12}-?\\d?",
                 message = "El NIT debe tener de 6 a 12 dígitos y, opcionalmente, el dígito de verificación (ej. 900100001-1)")
        String nit,

        @Pattern(regexp = "\\d{7,15}", message = "El teléfono debe tener entre 7 y 15 dígitos, sin espacios")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        @NotNull(message = "El tiempo de entrega es obligatorio")
        @PositiveOrZero(message = "El tiempo de entrega no puede ser negativo")
        Integer tiempoEntregaDias) {
}