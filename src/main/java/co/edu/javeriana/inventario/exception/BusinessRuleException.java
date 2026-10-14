package co.edu.javeriana.inventario.exception;

/**
 * Violacion de una regla de negocio (RN-02, RN-04, RN-06 a RN-11).
 * El GlobalExceptionHandler la traduce a 409 Conflict.
 *
 * Ademas del mensaje lleva una "accion correctiva": el RNF-03 exige
 * que el error diga tanto la causa como que debe hacer el usuario.
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
