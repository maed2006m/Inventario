package co.edu.javeriana.inventario.exception;

/**
 * Se lanza cuando la solicitud misma es inválida, aunque su formato JSON sea correcto:
 * por ejemplo, un rango de fechas invertido o demasiado largo (RN-15).
 * El manejador global la traduce a HTTP 400 Bad Request.
 *
 * Lleva la causa (message) y la acción correctiva (accion), como exige el RNF-03.
 */
public class SolicitudInvalidaException extends RuntimeException {

    private final String accion;

    public SolicitudInvalidaException(String mensaje, String accion) {
        super(mensaje);
        this.accion = accion;
    }

    public String getAccion() {
        return accion;
    }
}