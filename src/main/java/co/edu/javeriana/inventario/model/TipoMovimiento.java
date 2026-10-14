package co.edu.javeriana.inventario.model;

/**
 * Direccion de un movimiento de inventario.
 *
 * ENTRADA suma unidades al saldo, SALIDA las resta.
 * Coincide con el CHECK de movimientos.tipo en V1__esquema_inicial.sql.
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA
}
