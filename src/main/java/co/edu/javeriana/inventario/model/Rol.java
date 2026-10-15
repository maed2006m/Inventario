package co.edu.javeriana.inventario.model;

/**
 * Roles del sistema (RNF-09, RNF-10).
 *
 * TENDERO: opera el dia a dia (catalogo, movimientos, consultas).
 * ADMIN:   ademas edita e inactiva proveedores y accede a reportes.
 *
 * Los valores coinciden con el CHECK de la columna usuarios.rol
 * definido en V1__esquema_inicial.sql.
 */
public enum Rol {
    TENDERO,
    ADMIN
}
