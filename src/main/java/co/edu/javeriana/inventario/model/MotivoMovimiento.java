package co.edu.javeriana.inventario.model;

/**
 * Causa de un movimiento (RN-04).
 *
 * COMPRA solo puede acompanar a una ENTRADA.
 * VENTA, AVERIA y VENCIMIENTO solo pueden acompanar a una SALIDA.
 *
 * Esa correspondencia la hace cumplir el servicio y, como red de
 * seguridad, la restriccion ck_tipo_motivo de la base de datos.
 */
public enum MotivoMovimiento {
    COMPRA,
    VENTA,
    AVERIA,
    VENCIMIENTO
}
