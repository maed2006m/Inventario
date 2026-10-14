package co.edu.javeriana.inventario.model;

/**
 * Estado de existencia de un producto (RN-10, RF-23).
 *
 * AGOTADO     existencia = 0
 * BAJO_STOCK  0 < existencia <= stock minimo
 * EN_STOCK    existencia > stock minimo
 *
 * No se guarda en la base de datos: se calcula en el servicio a
 * partir de la existencia y el stock minimo del producto.
 */
public enum EstadoStock {
    EN_STOCK,
    BAJO_STOCK,
    AGOTADO
}
