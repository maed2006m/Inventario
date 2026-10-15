package co.edu.javeriana.inventario.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de participación de cada proveedor en el valor del inventario (RF-26, HU13).
 * Incluye el índice HHI, que mide qué tan concentrado está el inventario en pocos proveedores.
 *
 * Si el inventario está vacío (valor total 0), todas las participaciones son 0,
 * el HHI es 0 y el nivel es BAJA.
 *
 * @param valorTotalInventario suma de existencia por costo de todos los productos de proveedores activos
 * @param indiceHhi            suma de los cuadrados de las participaciones en porcentaje (de 0 a 10.000)
 * @param nivelConcentracion   BAJA (menos de 1.500), MODERADA (de 1.500 a 2.500) o ALTA (más de 2.500)
 * @param proveedores          una fila por proveedor, de mayor a menor participación
 */
public record ParticipacionResponse(
        BigDecimal valorTotalInventario,
        BigDecimal indiceHhi,
        NivelConcentracion nivelConcentracion,
        List<Fila> proveedores) {

    /** Nivel de riesgo por dependencia de pocos proveedores. */
    public enum NivelConcentracion {
        BAJA,
        MODERADA,
        ALTA
    }

    /**
     * Participación de un proveedor.
     *
     * @param valorInventario  existencia por costo de sus productos
     * @param participacionPct porcentaje que representa del total, de 0 a 100
     */
    public record Fila(
            Long proveedorId,
            String proveedorNombre,
            BigDecimal valorInventario,
            BigDecimal participacionPct) {
    }
}