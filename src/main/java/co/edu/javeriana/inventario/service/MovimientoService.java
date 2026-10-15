
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
 * Los movimientos son inmutables (RN-05): esta clase solo los crea,
 * nunca los edita ni los borra.
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final MovimientoMapper mapper;

    /**
     * Registra una entrada o una salida y devuelve la existencia
     * que quedó después del movimiento.
     *
     * @param request datos enviados por el cliente
     * @param username usuario autenticado, tomado de la sesión o token
     */
    @Transactional
    public MovimientoResponse registrar(
            MovimientoRequest request,
            String username) {

        validarSolicitud(request, username);
        validarMotivo(request.tipo(), request.motivo());

        /*
         * RN-14: bloquea la fila del producto hasta que termine
         * la transacción, evitando que dos operaciones concurrentes
         * utilicen simultáneamente la misma existencia.
         */
        Producto producto = productos.findByIdForUpdate(request.productoId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "El producto " + request.productoId()
                                        + " no existe."));

        validarProductoHabilitado(producto, request.motivo());

        /*
         * La existencia se calcula mientras la fila del producto
         * está bloqueada. Se mantiene el tipo int porque el DTO y
         * la respuesta actuales usan Integer.
         */
        long existenciaActual = movimientos.calcularExistencia(producto.getId());

        if (existenciaActual < 0 || existenciaActual > Integer.MAX_VALUE) {
            throw new BusinessRuleException(
                    "La existencia del producto '"
                            + producto.getNombre()
                            + "' está fuera del rango permitido.",
                    "Revise el historial de movimientos y corrija la inconsistencia.");
        }

        int disponible = (int) existenciaActual;

        if (request.tipo() == TipoMovimiento.SALIDA
                && request.cantidad() > disponible) {
            throw new StockInsuficienteException(
                    producto.getNombre(),
                    disponible,
                    request.cantidad());
        }

        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "El usuario '" + username + "' no existe."));

        /*
         * No se permite registrar movimientos con una cuenta
         * que se encuentre deshabilitada.
         */
        if (!usuario.isActivo()) {
            throw new BusinessRuleException(
                    "El usuario '" + username + "' está deshabilitado.",
                    "Solicite a un administrador que habilite la cuenta.");
        }

        Movimiento movimiento = mapper.aEntidad(
                request,
                producto,
                usuario,
                valorPorDefecto(request, producto));

        movimientos.save(movimiento);

        int resultante;

        if (request.tipo() == TipoMovimiento.ENTRADA) {
            resultante = disponible + request.cantidad();
        } else {
            resultante = disponible - request.cantidad();
        }

        return mapper.aRespuesta(movimiento, resultante);
    }

    /**
     * Comprueba que la solicitud y los datos obligatorios estén presentes.
     * Las anotaciones del DTO también deben mantenerse para validar
     * las solicitudes que llegan por los controladores.
     */
    private void validarSolicitud(
            MovimientoRequest request,
            String username) {

        if (request == null) {
            throw new BusinessRuleException(
                    "La solicitud del movimiento es obligatoria.",
                    "Envíe los datos del movimiento.");
        }

        if (request.productoId() == null) {
            throw new BusinessRuleException(
                    "El producto es obligatorio.",
                    "Seleccione un producto existente.");
        }

        if (request.tipo() == null) {
            throw new BusinessRuleException(
                    "El tipo de movimiento es obligatorio.",
                    "Seleccione ENTRADA o SALIDA.");
        }

        if (request.motivo() == null) {
            throw new BusinessRuleException(
                    "El motivo del movimiento es obligatorio.",
                    "Seleccione un motivo válido.");
        }

        if (request.cantidad() == null || request.cantidad() <= 0) {
            throw new BusinessRuleException(
                    "La cantidad debe ser mayor que cero.",
                    "Ingrese una cantidad positiva.");
        }

        if (request.valorUnitario() != null
                && request.valorUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "El valor unitario no puede ser negativo.",
                    "Ingrese un valor igual o mayor que cero.");
        }

        if (username == null || username.isBlank()) {
            throw new BusinessRuleException(
                    "No se pudo identificar al usuario que registra el movimiento.",
                    "Inicie sesión nuevamente.");
        }
    }

    /**
     * RN-04: las entradas solo pueden ser COMPRA.
     * Las salidas pueden ser VENTA, AVERIA o VENCIMIENTO.
     */
    private void validarMotivo(
            TipoMovimiento tipo,
            MotivoMovimiento motivo) {

        boolean coherente =
                (tipo == TipoMovimiento.ENTRADA
                        && motivo == MotivoMovimiento.COMPRA)
                || (tipo == TipoMovimiento.SALIDA
                        && (motivo == MotivoMovimiento.VENTA
                        || motivo == MotivoMovimiento.AVERIA
                        || motivo == MotivoMovimiento.VENCIMIENTO));

        if (!coherente) {
            throw new BusinessRuleException(
                    "El motivo " + motivo
                            + " no corresponde a una " + tipo + ".",
                    "Use COMPRA para entradas y VENTA, AVERIA o VENCIMIENTO para salidas.");
        }
    }

    /**
     * RN-08: un producto deshabilitado solo admite bajas
     * por avería o vencimiento.
     */
    private void validarProductoHabilitado(
            Producto producto,
            MotivoMovimiento motivo) {

        boolean esBaja =
                motivo == MotivoMovimiento.AVERIA
                || motivo == MotivoMovimiento.VENCIMIENTO;

        if (!producto.isActivo() && !esBaja) {
            throw new BusinessRuleException(
                    "El producto '" + producto.getNombre()
                            + "' está deshabilitado.",
                    "Habilítelo para registrar compras o ventas; "
                            + "solo se admiten bajas por avería o vencimiento.");
        }
    }

    /**
     * RN-13: usa el valor enviado si existe.
     * Si no se envía, utiliza el precio de venta para ventas
     * y el costo unitario para compras, averías y vencimientos.
     */
    private BigDecimal valorPorDefecto(
            MovimientoRequest request,
            Producto producto) {

        if (request.valorUnitario() != null) {
            return request.valorUnitario();
        }

        return request.motivo() == MotivoMovimiento.VENTA
                ? producto.getPrecioVenta()
                : producto.getCostoUnitario();
    }
}
