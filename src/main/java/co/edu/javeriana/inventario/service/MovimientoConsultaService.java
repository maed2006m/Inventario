package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.dto.PaginaResponse;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.exception.SolicitudInvalidaException;
import co.edu.javeriana.inventario.mapper.MovimientoMapper;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.MovimientoSpecs;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Consulta del historial de movimientos (RF-11, RF-27, RF-28).
 * Solo lee: los movimientos son inmutables y esta clase nunca los modifica (RN-05).
 */
@Service
@RequiredArgsConstructor
public class MovimientoConsultaService {

    /** Máximo de filas por página: protege el rendimiento (RNF-06). */
    static final int TAMANIO_MAXIMO = 100;

    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final MovimientoMapper mapper;

    /**
     * Historial general con filtros opcionales. Los filtros en null se ignoran.
     * Siempre se ordena del más reciente al más antiguo.
     *
     * @param pagina  número de página; la primera es la 0
     * @param tamanio filas por página, entre 1 y 100
     */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> buscar(Long productoId, TipoMovimiento tipo,
                                                     MotivoMovimiento motivo, String username,
                                                     OffsetDateTime desde, OffsetDateTime hasta,
                                                     int pagina, int tamanio) {
        validarRango(desde, hasta);
        Pageable pageable = paginar(pagina, tamanio);

        Page<Movimiento> resultado = movimientos.findAll(
                MovimientoSpecs.filtrar(productoId, tipo, motivo, username, desde, hasta), pageable);

        return PaginaResponse.de(resultado, m -> mapper.aRespuesta(m));
    }

    /** Historial de un solo producto (RF-11). Si el producto no existe, responde 404. */
    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoResponse> historialDeProducto(Long productoId, int pagina, int tamanio) {
        if (!productos.existsById(productoId)) {
            throw new ResourceNotFoundException("El producto " + productoId + " no existe.");
        }
        Page<Movimiento> resultado = movimientos.findByProductoId(productoId, paginar(pagina, tamanio));
        return PaginaResponse.de(resultado, m -> mapper.aRespuesta(m));
    }

    /** RN-15: si se envían ambas fechas, el inicio no puede ser posterior al fin. */
    private void validarRango(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    "La fecha inicial (" + desde + ") es posterior a la fecha final (" + hasta + ").",
                    "Corrija el rango: la fecha inicial debe ser anterior o igual a la final.");
        }
    }

    /**
     * Arma la paginación con un orden fijo: fecha descendente y, si empatan, id descendente.
     * El orden NO se deja a elección del cliente (ver explicación en la guía).
     */
    private Pageable paginar(int pagina, int tamanio) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(
                    "El número de página no puede ser negativo.",
                    "Use 0 para la primera página.");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new SolicitudInvalidaException(
                    "El tamaño de página debe estar entre 1 y " + TAMANIO_MAXIMO + ".",
                    "Envíe un tamaño entre 1 y " + TAMANIO_MAXIMO + ".");
        }
        Sort orden = Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pagina, tamanio, orden);
    }
}