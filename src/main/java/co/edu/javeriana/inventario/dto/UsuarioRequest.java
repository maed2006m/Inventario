package co.edu.javeriana.inventario.dto;

import co.edu.javeriana.inventario.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cuenta (RF-30). Solo el ADMIN puede enviarlos.
 *
 * El usuario se guarda en minusculas para que "Miguel" y "miguel" no
 * puedan ser dos cuentas distintas. La contrasena llega en claro por
 * HTTPS y el servicio la guarda solo como hash BCrypt (RNF-09).
 *
 * Maximo 72 caracteres en la contrasena: BCrypt ignora lo que pase de
 * 72 bytes, asi que aceptar mas daria una falsa sensacion de seguridad.
 */
public record UsuarioRequest(

        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
                message = "El usuario debe tener entre 3 y 50 caracteres: letras, numeros, punto, guion o guion bajo, sin espacios")
        String username,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
}
