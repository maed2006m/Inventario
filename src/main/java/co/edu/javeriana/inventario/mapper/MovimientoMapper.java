package co.edu.javeriana.inventario.mapper;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Usuario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Traduce entre el DTO de movimiento y la entidad Movimiento.
 * No contiene reglas de negocio ni consulta la base de datos: el servicio le entrega
 * el producto, el usuario y el valor unitario ya resueltos (RN-13).
 */
@Component
public class MovimientoMapper {

    /**
     * Crea la entidad a partir de lo que envió el cliente y de lo que el servicio resolvió.
     * No asigna la fecha: la entidad la pone al guardarse.
     */
    public Movimiento aEntidad(MovimientoRequest request, Producto producto, Usuario usuario,
                               BigDecimal valorUnitario) {
        Movimiento movimiento = new Movimiento();
        movimiento.setProducto(producto);
        movimiento.setTipo(request.tipo());
        movimiento.setMotivo(request.motivo());
        movimiento.setCantidad(request.cantidad());
        movimiento.setValorUnitario(valorUnitario);
        movimiento.setObservacion(request.observacion());
        movimiento.setUsuario(usuario);
        return movimiento;
    }

    /**
     * Respuesta al registrar un movimiento: incluye la existencia que quedó después de él.
     * Debe llamarse DESPUÉS de guardar, para que ya tenga id y fecha.
     */
    public MovimientoResponse aRespuesta(Movimiento movimiento, Integer existenciaResultante) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getNombre(),
                movimiento.getProducto().getProveedor().getNombre(),
                movimiento.getTipo(),
                movimiento.getMotivo(),
                movimiento.getCantidad(),
                movimiento.getValorUnitario(),
                movimiento.getObservacion(),
                movimiento.getUsuario().getUsername(),
                movimiento.getFecha(),
                existenciaResultante);
    }

    /** Respuesta para el historial: sin existencia resultante, que queda en null y no viaja en el JSON. */
    public MovimientoResponse aRespuesta(Movimiento movimiento) {
        return aRespuesta(movimiento, null);
    }
}