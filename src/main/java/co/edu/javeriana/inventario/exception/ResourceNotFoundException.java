package co.edu.javeriana.inventario.exception;

/**
 * Se lanza cuando un recurso buscado por su identificador no existe
 * (producto, proveedor, usuario). El manejador global la traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}