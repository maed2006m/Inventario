package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.*;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Nucleo del sistema: registro de entradas y salidas.
 *
 * Aqui viven las reglas RN-02, RN-03, RN-04, RN-08, RN-13 y RN-14.
 * Es la clase que mas peso tiene en las pruebas unitarias.
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    /**
     * Registra un movimiento y devuelve el saldo resultante.
     *
     * @Transactional es obligatorio: la lectura de la existencia y la
     * insercion del movimiento deben ocurrir dentro de la misma
     * transaccion, o el bloqueo de RN-14 no sirve de nada.
     */
    @Transactional
    public MovimientoResponse registrar(MovimientoRequest req, String username) {

        /*
         * RN-14: bloqueo pesimista.
         *
         * findByIdForUpdate genera SELECT ... FOR UPDATE y retiene la
         * fila del producto hasta que termine esta transaccion. Si dos
         * ventas del mismo producto llegan a la vez, la segunda espera
         * aqui y luego lee la existencia ya actualizada.
         *
         * Sin este bloqueo ambas leerian el mismo saldo, ambas pasarian
         * la validacion de RN-02 y el inventario quedaria negativo.
         */
        Producto producto = productos.findByIdForUpdate(req.productoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + req.productoId() + " no existe."));

        validarMotivo(req.tipo(), req.motivo());              // RN-04
        validarProductoHabilitado(producto, req.motivo());    // RN-08

        int disponible = (int) movimientos.calcularExistencia(producto.getId());

        // RN-02, RF-21, RF-22: una salida nunca deja el saldo negativo.
        if (req.tipo() == TipoMovimiento.SALIDA && req.cantidad() > disponible) {
            throw new BusinessRuleException(
                    "La salida de " + req.cantidad() + " unidades de '" + producto.getNombre()
                            + "' supera la existencia disponible (" + disponible + ").",
                    disponible > 0
                            ? "Reduzca la cantidad a " + disponible
                              + " o menos, o registre antes una entrada de compra."
                            : "El producto esta agotado: registre antes una entrada de compra.");
        }

        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + username + " no existe."));

        Movimiento m = new Movimiento();
        m.setProducto(producto);
        m.setTipo(req.tipo());
        m.setMotivo(req.motivo());
        m.setCantidad(req.cantidad());
        m.setValorUnitario(valorPorDefecto(req, producto));   // RN-13
        m.setObservacion(req.observacion());
        m.setUsuario(usuario);

        movimientos.save(m);

        int resultante = req.tipo() == TipoMovimiento.ENTRADA
                ? disponible + req.cantidad()
                : disponible - req.cantidad();

        return aRespuesta(m, resultante);
    }

    /**
     * RN-04: tipo y motivo deben corresponder.
     *
     * Una entrada solo puede ser una COMPRA. Una salida puede ser
     * VENTA, AVERIA o VENCIMIENTO, pero nunca COMPRA.
     */
    private void validarMotivo(TipoMovimiento tipo, MotivoMovimiento motivo) {

        boolean valido = (tipo == TipoMovimiento.ENTRADA && motivo == MotivoMovimiento.COMPRA)
                || (tipo == TipoMovimiento.SALIDA && motivo != MotivoMovimiento.COMPRA);

        if (!valido) {
            throw new BusinessRuleException(
                    "El motivo " + motivo + " no corresponde a una " + tipo + ".",
                    "Use COMPRA para entradas y VENTA, AVERIA o VENCIMIENTO para salidas.");
        }
    }

    /**
     * RN-08: un producto deshabilitado no admite compras ni ventas,
     * pero si bajas por averia o vencimiento.
     *
     * El motivo es practico: si se deja de comercializar un producto,
     * el remanente que quede en bodega todavia debe poder darse de
     * baja cuando se dane o se venza.
     */
    private void validarProductoHabilitado(Producto p, MotivoMovimiento motivo) {

        boolean esBaja = motivo == MotivoMovimiento.AVERIA || motivo == MotivoMovimiento.VENCIMIENTO;

        if (!p.isActivo() && !esBaja) {
            throw new BusinessRuleException(
                    "El producto '" + p.getNombre() + "' esta deshabilitado.",
                    "Habilitelo para registrar compras o ventas; solo se admiten bajas por averia o vencimiento.");
        }
    }

    /**
     * RN-13: valor unitario por defecto.
     *
     * Una venta se valora al precio de venta; cualquier otro
     * movimiento, al costo del producto.
     */
    private BigDecimal valorPorDefecto(MovimientoRequest req, Producto p) {
        if (req.valorUnitario() != null) {
            return req.valorUnitario();
        }
        return req.motivo() == MotivoMovimiento.VENTA ? p.getPrecioVenta() : p.getCostoUnitario();
    }

    private MovimientoResponse aRespuesta(Movimiento m, int existenciaResultante) {
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
                existenciaResultante);
    }
}
