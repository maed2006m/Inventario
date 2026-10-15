package co.edu.javeriana.inventario.exception;

/**
 * Se lanza cuando se pide un recurso que no existe.
 * El GlobalExceptionHandler la traduce a 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
