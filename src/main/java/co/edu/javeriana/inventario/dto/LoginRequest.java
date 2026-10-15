package co.edu.javeriana.inventario.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales que envia el cliente para iniciar sesion.
 */
public record LoginRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}
