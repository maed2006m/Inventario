package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.*;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las reglas de negocio del registro de
 * movimientos. No tocan la base de datos: los repositorios son mocks.
 *
 * Cada prueba lleva en el nombre la regla que verifica, para que el
 * informe pueda citarlas por numero.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock MovimientoRepository movimientos;
    @Mock ProductoRepository productos;
    @Mock UsuarioRepository usuarios;

    @InjectMocks MovimientoService service;

    Producto leche;

    @BeforeEach
    void preparar() {
        Proveedor prov = new Proveedor();
        prov.setId(2L);
        prov.setNombre("Lacteos del Valle");
        prov.setActivo(true);

        leche = new Producto();
        leche.setId(5L);
        leche.setNombre("Leche entera 1 L");
        leche.setActivo(true);
        leche.setProveedor(prov);
        leche.setStockMinimo(30);
        leche.setCostoUnitario(new BigDecimal("3100"));
        leche.setPrecioVenta(new BigDecimal("3900"));

        Usuario u = new Usuario();
        u.setId(2L);
        u.setUsername("tendero");
        u.setRol(Rol.TENDERO);

        // lenient() evita que Mockito se queje en las pruebas que
        // no llegan a usar estos stubs (por ejemplo, las que fallan
        // en una validacion anterior).
        lenient().when(productos.findByIdForUpdate(5L)).thenReturn(Optional.of(leche));
        lenient().when(usuarios.findByUsername("tendero")).thenReturn(Optional.of(u));
    }

    /**
     * RN-02, RF-21, RF-22: la prueba central del proyecto.
     *
     * Verifica dos cosas distintas: que se rechaza la salida Y que NO
     * se guarda nada. Un sistema que lanzara el error pero igual
     * insertara el movimiento pasaria la primera aserción y fallaria
     * la segunda.
     */
    @Test
    @DisplayName("RN-02: una salida mayor que la existencia se rechaza y no se guarda")
    void rn02_salidaMayorQueLaExistenciaSeRechaza() {

        when(movimientos.calcularExistencia(5L)).thenReturn(10L);

        var req = new MovimientoRequest(5L, TipoMovimiento.SALIDA,
                MotivoMovimiento.VENTA, 11, null, null);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.registrar(req, "tendero"));

        assertTrue(ex.getMessage().contains("(10)"),
                "El mensaje debe indicar la existencia disponible");
        assertTrue(ex.getAccion().contains("10"),
                "RNF-03: debe indicar la accion correctiva con la cantidad posible");

        verify(movimientos, never()).save(any());
    }

    /** RN-02: el caso limite. Vender exactamente lo disponible si se permite. */
    @Test
    @DisplayName("RN-02: una salida igual a la existencia se permite y deja el saldo en cero")
    void rn02_salidaIgualALaExistenciaSePermite() {

        when(movimientos.calcularExistencia(5L)).thenReturn(10L);

        var req = new MovimientoRequest(5L, TipoMovimiento.SALIDA,
                MotivoMovimiento.VENTA, 10, null, null);

        var respuesta = service.registrar(req, "tendero");

        assertEquals(0, respuesta.existenciaResultante());
    }

    /** RN-04: una entrada solo puede ser COMPRA. */
    @Test
    @DisplayName("RN-04: una entrada con motivo VENTA se rechaza")
    void rn04_entradaConMotivoVentaSeRechaza() {

        var req = new MovimientoRequest(5L, TipoMovimiento.ENTRADA,
                MotivoMovimiento.VENTA, 1, null, null);

        assertThrows(BusinessRuleException.class,
                () -> service.registrar(req, "tendero"));

        verify(movimientos, never()).save(any());
    }

    /** RN-08: deshabilitado no admite venta, pero si baja por averia. */
    @Test
    @DisplayName("RN-08: un producto deshabilitado no admite venta pero si averia")
    void rn08_productoDeshabilitado() {

        leche.setActivo(false);
        when(movimientos.calcularExistencia(5L)).thenReturn(5L);

        var venta = new MovimientoRequest(5L, TipoMovimiento.SALIDA,
                MotivoMovimiento.VENTA, 1, null, null);
        var averia = new MovimientoRequest(5L, TipoMovimiento.SALIDA,
                MotivoMovimiento.AVERIA, 1, null, null);

        assertThrows(BusinessRuleException.class,
                () -> service.registrar(venta, "tendero"));

        assertEquals(4, service.registrar(averia, "tendero").existenciaResultante());
    }

    /** RN-13: sin valor unitario, la entrada usa el costo del producto. */
    @Test
    @DisplayName("RN-13: una entrada sin valor unitario usa el costo del producto")
    void rn13_entradaUsaElCostoPorDefecto() {

        when(movimientos.calcularExistencia(5L)).thenReturn(0L);

        var req = new MovimientoRequest(5L, TipoMovimiento.ENTRADA,
                MotivoMovimiento.COMPRA, 20, null, null);

        service.registrar(req, "tendero");

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());

        assertEquals(0, new BigDecimal("3100").compareTo(captor.getValue().getValorUnitario()));
    }

    /** RN-13: sin valor unitario, la venta usa el precio de venta. */
    @Test
    @DisplayName("RN-13: una venta sin valor unitario usa el precio de venta")
    void rn13_ventaUsaElPrecioPorDefecto() {

        when(movimientos.calcularExistencia(5L)).thenReturn(50L);

        var req = new MovimientoRequest(5L, TipoMovimiento.SALIDA,
                MotivoMovimiento.VENTA, 2, null, null);

        service.registrar(req, "tendero");

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());

        assertEquals(0, new BigDecimal("3900").compareTo(captor.getValue().getValorUnitario()));
    }

    @Test
    @DisplayName("Un producto inexistente responde 404")
    void productoInexistenteLanza404() {

        when(productos.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        var req = new MovimientoRequest(99L, TipoMovimiento.ENTRADA,
                MotivoMovimiento.COMPRA, 1, null, null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(req, "tendero"));
    }
}
