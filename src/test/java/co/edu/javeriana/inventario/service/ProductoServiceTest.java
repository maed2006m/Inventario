package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProductoRequest;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas de negocio de productos: RN-06 (proveedor activo),
 * RN-09 (SKU unico) y RN-11 (integridad del historial).
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository productos;
    @Mock ProveedorRepository proveedores;
    @Mock MovimientoRepository movimientos;

    @InjectMocks ProductoService service;

    private Proveedor proveedor(boolean activo) {
        Proveedor p = new Proveedor();
        p.setId(1L);
        p.setNombre("Distribuidora Andina");
        p.setActivo(activo);
        return p;
    }

    private ProductoRequest request(String sku) {
        return new ProductoRequest(sku, "Arroz 500 g", "Granos", 1L, 20,
                new BigDecimal("1800"), new BigDecimal("2400"));
    }

    /** RN-06: no se crean productos con proveedores inactivos. */
    @Test
    @DisplayName("RN-06: crear un producto con proveedor inactivo se rechaza")
    void rn06_proveedorInactivoSeRechaza() {

        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor(false)));

        assertThrows(BusinessRuleException.class, () -> service.crear(request("ARR-001")));
        verify(productos, never()).save(any());
    }

    /** RN-09: el SKU es unico. */
    @Test
    @DisplayName("RN-09: crear un producto con SKU repetido se rechaza")
    void rn09_skuDuplicadoSeRechaza() {

        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor(true)));
        when(productos.existsBySku("ARR-001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.crear(request("ARR-001")));
        verify(productos, never()).save(any());
    }

    /**
     * RN-11: el SKU es la referencia con la que se registro el
     * historial. Si el producto ya tiene movimientos, cambiarlo
     * reescribiria el pasado.
     */
    @Test
    @DisplayName("RN-11: cambiar el SKU de un producto con movimientos se rechaza")
    void rn11_cambioDeSkuConMovimientosSeRechaza() {

        Producto p = new Producto();
        p.setId(5L);
        p.setSku("ARR-001");
        p.setProveedor(proveedor(true));

        when(productos.findById(5L)).thenReturn(Optional.of(p));
        when(movimientos.existsByProductoId(5L)).thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.actualizar(5L, request("ARR-002")));

        assertTrue(ex.getMessage().toLowerCase().contains("sku"));
        verify(productos, never()).save(any());
    }

    /** RN-11: sin movimientos, el SKU si se puede corregir. */
    @Test
    @DisplayName("RN-11: cambiar el SKU de un producto sin movimientos se permite")
    void rn11_cambioDeSkuSinMovimientosSePermite() {

        Producto p = new Producto();
        p.setId(5L);
        p.setSku("ARR-001");
        p.setStockMinimo(20);
        p.setProveedor(proveedor(true));

        when(productos.findById(5L)).thenReturn(Optional.of(p));
        when(movimientos.existsByProductoId(5L)).thenReturn(false);
        when(productos.existsBySkuAndIdNot("ARR-002", 5L)).thenReturn(false);
        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor(true)));
        when(productos.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = service.actualizar(5L, request("ARR-002"));

        assertEquals("ARR-002", respuesta.sku());
    }
}
