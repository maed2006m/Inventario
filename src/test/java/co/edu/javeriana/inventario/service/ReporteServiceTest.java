package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ParticipacionResponse;
import co.edu.javeriana.inventario.dto.ParticipacionResponse.NivelConcentracion;
import co.edu.javeriana.inventario.exception.SolicitudInvalidaException;
import co.edu.javeriana.inventario.service.ReporteService.ValorProveedor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calculos puros de ReporteService (HU13, RN-15): participacion por
 * proveedor, indice HHI y validacion del periodo.
 *
 * Son metodos static sin dependencias: se prueban sin Spring ni base de
 * datos. Los casos de borde (inventario en cero, umbrales exactos) son
 * dificiles de forzar por la API porque la base es compartida.
 */
class ReporteServiceTest {

    private static ValorProveedor valor(long id, String valor) {
        return new ValorProveedor(id, "Proveedor " + id, new BigDecimal(valor));
    }

    @Test
    @DisplayName("HU13: con el inventario en cero no divide por cero y la concentracion es BAJA")
    void participacionConInventarioEnCero() {
        ParticipacionResponse r = ReporteService.calcularParticipacion(
                List.of(valor(1, "0"), valor(2, "0")));

        assertEquals(0, r.valorTotalInventario().signum());
        assertEquals(new BigDecimal("0.00"), r.indiceHhi());
        assertEquals(NivelConcentracion.BAJA, r.nivelConcentracion());
        assertEquals(2, r.proveedores().size());
        r.proveedores().forEach(f -> assertEquals(new BigDecimal("0.00"), f.participacionPct()));
    }

    @Test
    @DisplayName("HU13: sin proveedores el reporte sale vacio y en BAJA")
    void participacionSinProveedores() {
        ParticipacionResponse r = ReporteService.calcularParticipacion(List.of());

        assertTrue(r.proveedores().isEmpty());
        assertEquals(NivelConcentracion.BAJA, r.nivelConcentracion());
    }

    @Test
    @DisplayName("HU13: cinco proveedores con el mismo valor suman HHI 2000, concentracion MODERADA")
    void participacionRepartidaEnCinco() {
        ParticipacionResponse r = ReporteService.calcularParticipacion(List.of(
                valor(1, "100"), valor(2, "100"), valor(3, "100"), valor(4, "100"), valor(5, "100")));

        assertEquals(new BigDecimal("500"), r.valorTotalInventario());
        assertEquals(new BigDecimal("2000.00"), r.indiceHhi());
        assertEquals(NivelConcentracion.MODERADA, r.nivelConcentracion());
        r.proveedores().forEach(f -> assertEquals(new BigDecimal("20.00"), f.participacionPct()));
    }

    @Test
    @DisplayName("HU13: un solo proveedor concentra el 100 %, HHI 10000, concentracion ALTA")
    void participacionDeUnSoloProveedor() {
        ParticipacionResponse r = ReporteService.calcularParticipacion(List.of(valor(1, "350000")));

        assertEquals(new BigDecimal("10000.00"), r.indiceHhi());
        assertEquals(NivelConcentracion.ALTA, r.nivelConcentracion());
        assertEquals(new BigDecimal("100.00"), r.proveedores().get(0).participacionPct());
    }

    @Test
    @DisplayName("HU13: umbrales del HHI: menos de 1500 BAJA, de 1500 a 2500 MODERADA, mas de 2500 ALTA")
    void umbralesDelHhi() {
        assertEquals(NivelConcentracion.BAJA, ReporteService.nivelDe(new BigDecimal("1499.99")));
        assertEquals(NivelConcentracion.MODERADA, ReporteService.nivelDe(new BigDecimal("1500.00")));
        assertEquals(NivelConcentracion.MODERADA, ReporteService.nivelDe(new BigDecimal("2500.00")));
        assertEquals(NivelConcentracion.ALTA, ReporteService.nivelDe(new BigDecimal("2500.01")));
    }

    @Test
    @DisplayName("RN-15: el periodo exige ambas fechas")
    void periodoSinFechas() {
        LocalDate hoy = LocalDate.of(2026, 6, 1);

        SolicitudInvalidaException sinDesde = assertThrows(SolicitudInvalidaException.class,
                () -> ReporteService.validarPeriodo(null, hoy));
        assertNotNull(sinDesde.getAccion());

        assertThrows(SolicitudInvalidaException.class, () -> ReporteService.validarPeriodo(hoy, null));
    }

    @Test
    @DisplayName("RN-15: 366 dias es el maximo permitido; 367 se rechaza")
    void periodoMaximo() {
        // 2024 es bisiesto: del 1 de enero al 31 de diciembre son 366 dias
        assertDoesNotThrow(() -> ReporteService.validarPeriodo(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)));

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> ReporteService.validarPeriodo(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)));
        assertTrue(e.getMessage().contains("367"));
    }

    @Test
    @DisplayName("RN-15: un periodo de un solo dia es valido")
    void periodoDeUnDia() {
        LocalDate dia = LocalDate.of(2026, 6, 1);
        assertDoesNotThrow(() -> ReporteService.validarPeriodo(dia, dia));
    }
}
