package co.edu.javeriana.inventario.exception;

/**
 * Login fallido: usuario inexistente, inactivo o contrasena incorrecta.
 *
 * Los tres casos comparten el mismo mensaje a proposito. Si el error
 * distinguiera "el usuario no existe" de "la contrasena es incorrecta",
 * cualquiera podria averiguar que usuarios existen en el sistema
 * probando nombres uno por uno.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o contrasena incorrectos.");
    }
}
