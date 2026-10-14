package co.edu.javeriana.inventario.exception;

/**
 * Se lanza cuando una salida pide más unidades que la existencia disponible (RN-02).
 *
 * Construye sola el mensaje y la acción correctiva, de modo que el usuario sepa
 * cuánto hay y qué hacer (RF-22, RNF-03).
 */
public class StockInsuficienteException extends BusinessRuleException {

    public StockInsuficienteException(String producto, int disponible, int solicitado) {
        super(
            "La salida de " + solicitado + " unidades de '" + producto
                + "' supera la existencia disponible (" + disponible + ").",
            disponible > 0
                ? "Reduzca la cantidad a " + disponible + " o menos, o registre antes una entrada de compra."
                : "El producto está agotado: registre antes una entrada de compra."
        );
    }
}