package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Consultas del historial (HU14, RF-27, RF-28).
 *
 * Se separa de MovimientoService a proposito: aquel registra y aplica
 * reglas de escritura, este solo lee. Mantenerlos aparte hace que las
 * pruebas de cada uno sean mas simples.
 */
@Service
@RequiredArgsConstructor
public class MovimientoConsultaService {

    /** RN-15: tope de rango para no permitir consultas desmedidas. */
    private static final long MAX_DIAS = 366;

    private final MovimientoRepository movimientos;

    @Transactional(readOnly = true)
    public Page<MovimientoResponse> buscar(Long productoId, Long proveedorId,
                                           TipoMovimiento tipo, MotivoMovimiento motivo,
                                           OffsetDateTime desde, OffsetDateTime hasta,
                                           Pageable pageable) {

        validarRango(desde, hasta);

        return movimientos
                .findAll(MovimientoSpecs.filtrar(productoId, proveedorId, tipo, motivo, desde, hasta), pageable)
                .map(this::aRespuesta);
    }

    /**
     * RN-15: la fecha inicial no puede ser posterior a la final, y el
     * rango no puede superar 366 dias.
     */
    private void validarRango(OffsetDateTime desde, OffsetDateTime hasta) {

        if (desde == null || hasta == null) {
            return;
        }

        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException(
                    "La fecha inicial es posterior a la fecha final.",
                    "Invierta las fechas del filtro.");
        }

        if (Duration.between(desde, hasta).toDays() > MAX_DIAS) {
            throw new BusinessRuleException(
                    "El rango de fechas supera " + MAX_DIAS + " dias.",
                    "Consulte un periodo mas corto.");
        }
    }

    /**
     * La existencia resultante no se calcula en el historial: habria
     * que recorrer todos los movimientos anteriores de cada fila. Se
     * deja en null y el frontend la consulta aparte si la necesita.
     */
    private MovimientoResponse aRespuesta(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getProducto().getId(),
                m.getProducto().getNombre(),
                m.getProducto().getProveedor().getNombre(),
                m.getTipo(),
                m.getMotivo(),
                m.getCantidad(),
                m.getValorUnitario(),
                m.getObservacion(),
                m.getUsuario().getUsername(),
                m.getFecha(),
                null);
    }
}
