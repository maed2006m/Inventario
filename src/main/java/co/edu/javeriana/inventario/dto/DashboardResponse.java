package co.edu.javeriana.inventario.dto;

import java.math.BigDecimal;

/**
 * Resumen del tablero principal (RF-23, Ilustración 3 del informe).
 *
 * Todos los valores son del momento de la consulta. El día y el mes se miden
 * en la zona horaria de la tienda (America/Bogota).
 *
 * @param productosActivos   productos habilitados en el catálogo
 * @param productosBajoStock productos activos con existencia mayor que 0 y menor o igual al stock mínimo (RN-10)
 * @param productosAgotados  productos activos con existencia 0 (RN-10)
 * @param movimientosHoy     entradas y salidas registradas desde las 00:00 de hoy
 * @param valorInventario    existencia por costo de los productos activos; coincide con el total del reporte de participación
 * @param ventasDelMes       dinero vendido desde el primer día del mes (solo salidas por VENTA)
 */
public record DashboardResponse(
        long productosActivos,
        long productosBajoStock,
        long productosAgotados,
        long movimientosHoy,
        BigDecimal valorInventario,
        BigDecimal ventasDelMes) {
}