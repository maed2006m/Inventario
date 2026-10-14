package co.edu.javeriana.inventario.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio (RN-02 a RN-11).
 * El manejador global la traduce a HTTP 409 Conflict.
 *
 * Lleva dos textos: la causa del problema (message) y la acción que el usuario
 * puede tomar para corregirlo (accion), como exige el RNF-03.
 */
public class BusinessRuleException extends RuntimeException {

    private final String accion;

    public BusinessRuleException(String mensaje, String accion) {
        super(mensaje);
        this.accion = accion;
    }

    public String getAccion() {
        return accion;
    }
}