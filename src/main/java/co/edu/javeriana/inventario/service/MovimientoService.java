package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.MovimientoResponse;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.exception.StockInsuficienteException;
import co.edu.javeriana.inventario.mapper.MovimientoMapper;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Registro de movimientos de inventario (RF-14 a RF-22).
 * Aplica RN-02, RN-04, RN-08, RN-13 y RN-14.
 *
 * Los movimientos son inmutables (RN-05): esta clase solo los crea, nunca los edita ni los borra.
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final MovimientoMapper mapper;

    /**
     * Registra una entrada o una salida y devuelve la existencia que quedó después de ella.
     *
     * @param request  datos enviados por el cliente
     * @param username usuario autenticado, tomado del token (nunca del cuerpo de la petición)
     */
    @Transactional
    public MovimientoResponse registrar(MovimientoRequest request, String username) {
        validarMotivo(request.tipo(), request.motivo());                                  // RN-04

        // RN-14: bloquea la fila del producto hasta que termine la transacción.
        Producto producto = productos.findByIdForUpdate(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + request.productoId() + " no existe."));

        validarProductoHabilitado(producto, request.motivo());                            // RN-08

        // Con la fila bloqueada, esta existencia es la real: nadie la puede cambiar mientras tanto.
        int disponible = (int) movimientos.calcularExistencia(producto.getId());

        if (request.tipo() == TipoMovimiento.SALIDA && request.cantidad() > disponible) { // RN-02
            throw new StockInsuficienteException(producto.getNombre(), disponible, request.cantidad());
        }

        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario '" + username + "' no existe."));

        Movimiento movimiento = mapper.aEntidad(request, producto, usuario,
                valorPorDefecto(request, producto));                                      // RN-13
        movimientos.save(movimiento);

        int resultante = request.tipo() == TipoMovimiento.ENTRADA
                ? disponible + request.cantidad()
                : disponible - request.cantidad();

        return mapper.aRespuesta(movimiento, resultante);
    }

    /** RN-04: las entradas solo son COMPRA; las salidas son VENTA, AVERIA o VENCIMIENTO. */
    private void validarMotivo(TipoMovimiento tipo, MotivoMovimiento motivo) {
        boolean coherente = (tipo == TipoMovimiento.ENTRADA && motivo == MotivoMovimiento.COMPRA)
                || (tipo == TipoMovimiento.SALIDA && motivo != MotivoMovimiento.COMPRA);
        if (!coherente) {
            throw new BusinessRuleException(
                    "El motivo " + motivo + " no corresponde a una " + tipo + ".",
                    "Use COMPRA para entradas y VENTA, AVERIA o VENCIMIENTO para salidas.");
        }
    }

    /** RN-08: un producto deshabilitado solo admite bajas por avería o vencimiento. */
    private void validarProductoHabilitado(Producto producto, MotivoMovimiento motivo) {
        boolean esBaja = motivo == MotivoMovimiento.AVERIA || motivo == MotivoMovimiento.VENCIMIENTO;
        if (!producto.isActivo() && !esBaja) {
            throw new BusinessRuleException(
                    "El producto '" + producto.getNombre() + "' está deshabilitado.",
                    "Habilítelo para registrar compras o ventas; solo se admiten bajas por avería o vencimiento.");
        }
    }

    /** RN-13: valor enviado, o precio de venta para ventas, o costo para compras y bajas. */
    private BigDecimal valorPorDefecto(MovimientoRequest request, Producto producto) {
        if (request.valorUnitario() != null) {
            return request.valorUnitario();
        }
        return request.motivo() == MotivoMovimiento.VENTA
                ? producto.getPrecioVenta()
                : producto.getCostoUnitario();
    }
}