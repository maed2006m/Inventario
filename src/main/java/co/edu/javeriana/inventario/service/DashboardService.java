package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumen del tablero principal (RF-23, Ilustración 3 del informe).
 * Se arma con dos consultas agregadas, no con un recorrido producto por producto (RNF-06).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /**
     * Estado del catálogo según RN-10. Un producto agotado (existencia 0) no se cuenta
     * como bajo stock: son grupos distintos. El valor del inventario usa el mismo criterio
     * que el reporte de participación: productos activos con proveedor activo.
     */
    private static final String SQL_CATALOGO = """
        SELECT COUNT(*) AS activos,
               COUNT(*) FILTER (WHERE v.existencia = 0) AS agotados,
               COUNT(*) FILTER (WHERE v.existencia > 0 AND v.existencia <= p.stock_minimo) AS bajo_stock,
               COALESCE(SUM(GREATEST(v.existencia, 0) * p.costo_unitario) FILTER (WHERE pr.activo), 0) AS valor_inventario
        FROM productos p
        JOIN proveedores pr ON pr.id = p.proveedor_id
        JOIN v_stock_producto v ON v.producto_id = p.id
        WHERE p.activo
        """;

    /** Actividad reciente: movimientos desde las 00:00 de hoy y ventas desde el día 1 del mes. */
    private static final String SQL_ACTIVIDAD = """
        SELECT COUNT(*) FILTER (WHERE fecha >= :inicioHoy) AS movimientos_hoy,
               COALESCE(SUM(cantidad * valor_unitario)
                        FILTER (WHERE tipo = 'SALIDA' AND motivo = 'VENTA'), 0) AS ventas_mes
        FROM movimientos
        WHERE fecha >= :inicioMes
        """;

    private final NamedParameterJdbcTemplate jdbc;

    /** Resumen con la fecha de hoy en la zona horaria de la tienda. */
    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        return resumen(LocalDate.now(ReporteService.ZONA));
    }

    /**
     * Resumen para un día dado. Existe aparte para poder probarlo con una fecha fija,
     * sin depender del reloj del computador.
     */
    DashboardResponse resumen(LocalDate hoy) {
        Catalogo catalogo = jdbc.getJdbcOperations().queryForObject(SQL_CATALOGO, (rs, fila) -> new Catalogo(
                rs.getLong("activos"),
                rs.getLong("bajo_stock"),
                rs.getLong("agotados"),
                rs.getBigDecimal("valor_inventario")));

        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("inicioHoy", hoy.atStartOfDay(ReporteService.ZONA).toOffsetDateTime())
                .addValue("inicioMes", hoy.withDayOfMonth(1).atStartOfDay(ReporteService.ZONA).toOffsetDateTime());

        Actividad actividad = jdbc.queryForObject(SQL_ACTIVIDAD, parametros, (rs, fila) -> new Actividad(
                rs.getLong("movimientos_hoy"),
                rs.getBigDecimal("ventas_mes")));

        return new DashboardResponse(
                catalogo.activos(),
                catalogo.bajoStock(),
                catalogo.agotados(),
                actividad.movimientosHoy(),
                catalogo.valorInventario(),
                actividad.ventasMes());
    }

    /** Resultado de la primera consulta. */
    private record Catalogo(long activos, long bajoStock, long agotados, BigDecimal valorInventario) {
    }

    /** Resultado de la segunda consulta. */
    private record Actividad(long movimientosHoy, BigDecimal ventasMes) {
    }
}