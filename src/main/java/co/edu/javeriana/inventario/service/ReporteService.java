package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ParticipacionResponse;
import co.edu.javeriana.inventario.dto.ParticipacionResponse.Fila;
import co.edu.javeriana.inventario.dto.ParticipacionResponse.NivelConcentracion;
import co.edu.javeriana.inventario.dto.RotacionResponse;
import co.edu.javeriana.inventario.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes para el administrador (RF-25, RF-26, HU12, HU13).
 * Las sumas pesadas se hacen en SQL; las fórmulas (participación, HHI) se calculan en Java
 * para poder probarlas con pruebas unitarias sin base de datos.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    /** RN-15: un reporte no puede abarcar más de este número de días. */
    static final int DIAS_MAXIMOS = 366;

    /** Zona horaria de la tienda: define dónde empieza y termina cada día. */
    static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal UMBRAL_MODERADA = BigDecimal.valueOf(1500);
    private static final BigDecimal UMBRAL_ALTA = BigDecimal.valueOf(2500);

    private static final String SQL_ROTACION = """
        WITH ventas AS (
            SELECT producto_id, SUM(cantidad) AS unidades, SUM(cantidad * valor_unitario) AS ingresos
            FROM movimientos
            WHERE tipo = 'SALIDA' AND motivo = 'VENTA' AND fecha >= :desde AND fecha < :hasta
            GROUP BY producto_id),
        stock_ini AS (
            SELECT producto_id, SUM(CASE WHEN tipo = 'ENTRADA' THEN cantidad ELSE -cantidad END) AS ex
            FROM movimientos WHERE fecha < :desde GROUP BY producto_id),
        stock_fin AS (
            SELECT producto_id, SUM(CASE WHEN tipo = 'ENTRADA' THEN cantidad ELSE -cantidad END) AS ex
            FROM movimientos WHERE fecha < :hasta GROUP BY producto_id)
        SELECT p.id AS producto_id, p.nombre AS producto_nombre, pr.nombre AS proveedor_nombre,
               COALESCE(v.unidades, 0) AS unidades_vendidas,
               COALESCE(v.ingresos, 0) AS ingresos,
               ROUND(COALESCE(v.unidades, 0)::numeric
                     / NULLIF((COALESCE(si.ex, 0) + COALESCE(sf.ex, 0)) / 2.0, 0), 2) AS indice_rotacion,
               RANK() OVER (ORDER BY COALESCE(v.unidades, 0) DESC) AS posicion
        FROM productos p
        JOIN proveedores pr ON pr.id = p.proveedor_id
        LEFT JOIN ventas v ON v.producto_id = p.id
        LEFT JOIN stock_ini si ON si.producto_id = p.id
        LEFT JOIN stock_fin sf ON sf.producto_id = p.id
        WHERE p.activo
        ORDER BY unidades_vendidas DESC, p.nombre
        """;

    private static final String SQL_VALOR_POR_PROVEEDOR = """
        SELECT pr.id AS proveedor_id, pr.nombre AS proveedor_nombre,
               SUM(GREATEST(v.existencia, 0) * p.costo_unitario) AS valor
        FROM productos p
        JOIN proveedores pr ON pr.id = p.proveedor_id
        JOIN v_stock_producto v ON v.producto_id = p.id
        WHERE p.activo AND pr.activo
        GROUP BY pr.id, pr.nombre
        ORDER BY valor DESC, pr.nombre
        """;

    private final NamedParameterJdbcTemplate jdbc;

    /** Valor del inventario de un proveedor. Fila intermedia entre la consulta SQL y el cálculo. */
    record ValorProveedor(Long proveedorId, String proveedorNombre, BigDecimal valor) {
    }

    /**
     * Ranking de rotación de productos activos entre dos fechas, ambas incluidas (RF-25).
     * El primero de la lista es el producto más vendido del periodo.
     */
    @Transactional(readOnly = true)
    public List<RotacionResponse> rotacion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);

        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("desde", desde.atStartOfDay(ZONA).toOffsetDateTime())
                // 'hasta' es exclusivo en la consulta: se suma un día para incluir el último día completo
                .addValue("hasta", hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime());

        return jdbc.query(SQL_ROTACION, parametros, (rs, fila) -> new RotacionResponse(
                rs.getInt("posicion"),
                rs.getLong("producto_id"),
                rs.getString("producto_nombre"),
                rs.getString("proveedor_nombre"),
                rs.getLong("unidades_vendidas"),
                rs.getBigDecimal("ingresos"),
                rs.getBigDecimal("indice_rotacion")));
    }

    /** Participación de cada proveedor en el valor del inventario actual, con el índice HHI (RF-26). */
    @Transactional(readOnly = true)
    public ParticipacionResponse participacion() {
        List<ValorProveedor> valores = jdbc.query(SQL_VALOR_POR_PROVEEDOR, (rs, fila) -> new ValorProveedor(
                rs.getLong("proveedor_id"),
                rs.getString("proveedor_nombre"),
                rs.getBigDecimal("valor")));
        return calcularParticipacion(valores);
    }

    /**
     * Calcula porcentajes y HHI a partir del valor de cada proveedor.
     * No toca la base de datos, por eso se puede probar directamente.
     */
    static ParticipacionResponse calcularParticipacion(List<ValorProveedor> valores) {
        BigDecimal total = valores.stream()
                .map(ValorProveedor::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Inventario vacío: evita dividir por cero.
        if (total.signum() == 0) {
            List<Fila> ceros = valores.stream()
                    .map(v -> new Fila(v.proveedorId(), v.proveedorNombre(), v.valor(), BigDecimal.ZERO.setScale(2)))
                    .toList();
            return new ParticipacionResponse(total, BigDecimal.ZERO.setScale(2), NivelConcentracion.BAJA, ceros);
        }

        BigDecimal hhi = BigDecimal.ZERO;
        List<Fila> filas = new ArrayList<>();
        for (ValorProveedor v : valores) {
            BigDecimal pctExacto = v.valor().multiply(CIEN).divide(total, 10, RoundingMode.HALF_UP);
            hhi = hhi.add(pctExacto.pow(2));
            filas.add(new Fila(v.proveedorId(), v.proveedorNombre(), v.valor(),
                    pctExacto.setScale(2, RoundingMode.HALF_UP)));
        }
        hhi = hhi.setScale(2, RoundingMode.HALF_UP);

        return new ParticipacionResponse(total, hhi, nivelDe(hhi), filas);
    }

    /** Menos de 1.500 es BAJA; de 1.500 a 2.500, MODERADA; más de 2.500, ALTA. */
    static NivelConcentracion nivelDe(BigDecimal hhi) {
        if (hhi.compareTo(UMBRAL_MODERADA) < 0) {
            return NivelConcentracion.BAJA;
        }
        return hhi.compareTo(UMBRAL_ALTA) <= 0 ? NivelConcentracion.MODERADA : NivelConcentracion.ALTA;
    }

    /** RN-15: ambas fechas son obligatorias, el inicio no puede ser posterior al fin y el máximo es 366 días. */
    static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException(
                    "El reporte exige una fecha inicial y una fecha final.",
                    "Envíe los parámetros 'desde' y 'hasta' con formato AAAA-MM-DD.");
        }
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    "La fecha inicial (" + desde + ") es posterior a la fecha final (" + hasta + ").",
                    "Corrija el rango: la fecha inicial debe ser anterior o igual a la final.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > DIAS_MAXIMOS) {
            throw new SolicitudInvalidaException(
                    "El periodo solicitado abarca " + dias + " días y el máximo permitido es " + DIAS_MAXIMOS + ".",
                    "Reduzca el rango a " + DIAS_MAXIMOS + " días o menos.");
        }
    }
}