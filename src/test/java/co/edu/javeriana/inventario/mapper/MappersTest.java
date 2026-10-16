package co.edu.javeriana.inventario.mapper;

import co.edu.javeriana.inventario.dto.MovimientoRequest;
import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.model.EstadoStock;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de los mappers: solo copian campos, no tienen reglas de negocio.
 */
class MappersTest {

    // ---------- ProductoMapper ----------

    private final ProductoMapper productoMapper = new ProductoMapper();

    @Test
    @DisplayName("ProductoMapper: crea la entidad con los datos del request y nace activa")
    void productoAEntidad() {
        ProductoRequest request = mock(ProductoRequest.class);
        when(request.sku()).thenReturn("SKU-1");
        when(request.nombre()).thenReturn("Arroz");
        Proveedor proveedor = new Proveedor();

        Producto producto = productoMapper.aEntidad(request, proveedor);

        assertEquals("SKU-1", producto.getSku());
        assertEquals("Arroz", producto.getNombre());
        assertSame(proveedor, producto.getProveedor());
        assertTrue(producto.isActivo());
    }

    @Test
    @DisplayName("ProductoMapper: actualizar copia los datos pero no cambia el estado activo")
    void productoActualizar() {
        ProductoRequest request = mock(ProductoRequest.class);
        when(request.sku()).thenReturn("SKU-2");
        when(request.nombre()).thenReturn("Leche");
        Producto producto = new Producto();
        producto.setActivo(false);
        Proveedor proveedor = new Proveedor();

        productoMapper.actualizar(producto, request, proveedor);

        assertEquals("SKU-2", producto.getSku());
        assertEquals("Leche", producto.getNombre());
        assertSame(proveedor, producto.getProveedor());
        assertFalse(producto.isActivo());
    }

    @Test
    @DisplayName("ProductoMapper: arma la respuesta con existencia y estado calculados")
    void productoARespuesta() {
        Proveedor proveedor = mock(Proveedor.class);
        when(proveedor.getId()).thenReturn(3L);
        when(proveedor.getNombre()).thenReturn("Distribuidora");
        Producto producto = mock(Producto.class);
        when(producto.getNombre()).thenReturn("Arroz");
        when(producto.getProveedor()).thenReturn(proveedor);

        assertNotNull(productoMapper.aRespuesta(producto, 5, EstadoStock.values()[0]));
    }

    // ---------- MovimientoMapper ----------

    private final MovimientoMapper movimientoMapper = new MovimientoMapper();

    @Test
    @DisplayName("MovimientoMapper: crea la entidad con el producto, el usuario y el valor resueltos")
    void movimientoAEntidad() {
        MovimientoRequest request = mock(MovimientoRequest.class);
        when(request.tipo()).thenReturn(TipoMovimiento.values()[0]);
        when(request.motivo()).thenReturn(MotivoMovimiento.values()[0]);
        when(request.observacion()).thenReturn("sin novedad");
        Producto producto = mock(Producto.class);
        Usuario usuario = mock(Usuario.class);
        BigDecimal valor = new BigDecimal("1500");

        Movimiento movimiento = movimientoMapper.aEntidad(request, producto, usuario, valor);

        assertSame(producto, movimiento.getProducto());
        assertSame(usuario, movimiento.getUsuario());
        assertEquals(TipoMovimiento.values()[0], movimiento.getTipo());
        assertEquals(MotivoMovimiento.values()[0], movimiento.getMotivo());
        assertEquals("sin novedad", movimiento.getObservacion());
        assertEquals(valor, movimiento.getValorUnitario());
    }

    @Test
    @DisplayName("MovimientoMapper: arma la respuesta con y sin existencia resultante")
    void movimientoARespuesta() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("tendero");
        Proveedor proveedor = mock(Proveedor.class);
        when(proveedor.getNombre()).thenReturn("Distribuidora");
        Producto producto = mock(Producto.class);
        when(producto.getId()).thenReturn(1L);
        when(producto.getNombre()).thenReturn("Arroz");
        when(producto.getProveedor()).thenReturn(proveedor);
        Movimiento movimiento = mock(Movimiento.class);
        when(movimiento.getProducto()).thenReturn(producto);
        when(movimiento.getUsuario()).thenReturn(usuario);

        assertNotNull(movimientoMapper.aRespuesta(movimiento, 7));
        assertNotNull(movimientoMapper.aRespuesta(movimiento));
    }

    // ---------- ProveedorMapper ----------

    private final ProveedorMapper proveedorMapper = new ProveedorMapper();

    @Test
    @DisplayName("ProveedorMapper: crea la entidad con los datos del request y nace activa")
    void proveedorAEntidad() {
        ProveedorRequest request = mock(ProveedorRequest.class);
        when(request.nombre()).thenReturn("Distribuidora");
        when(request.nit()).thenReturn("900123456");

        Proveedor proveedor = proveedorMapper.aEntidad(request);

        assertEquals("Distribuidora", proveedor.getNombre());
        assertEquals("900123456", proveedor.getNit());
        assertTrue(proveedor.isActivo());
    }

    @Test
    @DisplayName("ProveedorMapper: actualizar copia los datos pero no cambia el estado activo")
    void proveedorActualizar() {
        ProveedorRequest request = mock(ProveedorRequest.class);
        when(request.nombre()).thenReturn("Nuevo nombre");
        Proveedor proveedor = new Proveedor();
        proveedor.setActivo(false);

        proveedorMapper.actualizar(proveedor, request);

        assertEquals("Nuevo nombre", proveedor.getNombre());
        assertFalse(proveedor.isActivo());
    }

    @Test
    @DisplayName("ProveedorMapper: arma la respuesta con el conteo de productos activos")
    void proveedorARespuesta() {
        Proveedor proveedor = mock(Proveedor.class);
        when(proveedor.getId()).thenReturn(1L);
        when(proveedor.getNombre()).thenReturn("Distribuidora");

        assertNotNull(proveedorMapper.aRespuesta(proveedor, 4L));
    }
}