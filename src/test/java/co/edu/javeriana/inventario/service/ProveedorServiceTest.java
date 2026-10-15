package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.dto.ProveedorRequest;
import co.edu.javeriana.inventario.exception.BusinessRuleException;
import co.edu.javeriana.inventario.exception.ResourceNotFoundException;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.repository.ProductoRepository;
import co.edu.javeriana.inventario.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas de negocio de proveedores: RN-07 (eliminacion logica) y
 * RN-09 (unicidad del NIT).
 */
@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock ProveedorRepository proveedores;
    @Mock ProductoRepository productos;

    @InjectMocks ProveedorService service;

    private Proveedor andina() {
        Proveedor p = new Proveedor();
        p.setId(1L);
        p.setNombre("Distribuidora Andina");
        p.setNit("900100001-1");
        p.setActivo(true);
        p.setTiempoEntregaDias(2);
        return p;
    }

    /**
     * RN-07: no se puede inactivar un proveedor con productos activos.
     *
     * El mensaje debe incluir cuantos productos lo impiden, para que el
     * usuario sepa que tiene que resolver (RNF-03).
     */
    @Test
    @DisplayName("RN-07: inactivar un proveedor con productos activos se rechaza")
    void rn07_inactivarConProductosActivosSeRechaza() {

        when(proveedores.findById(1L)).thenReturn(Optional.of(andina()));
        when(productos.countByProveedorIdAndActivoTrue(1L)).thenReturn(3L);

        var ex = assertThrows(BusinessRuleException.class, () -> service.inactivar(1L));

        assertTrue(ex.getMessage().contains("3"),
                "El mensaje debe decir cuantos productos activos tiene");
        verify(proveedores, never()).save(any());
    }

    /** RN-07: sin productos activos, se inactiva (no se borra). */
    @Test
    @DisplayName("RN-07: inactivar marca activo en false, no elimina la fila")
    void rn07_inactivarSinProductosMarcaInactivo() {

        Proveedor p = andina();
        when(proveedores.findById(1L)).thenReturn(Optional.of(p));
        when(productos.countByProveedorIdAndActivoTrue(1L)).thenReturn(0L);

        service.inactivar(1L);

        assertFalse(p.isActivo());
        verify(proveedores).save(p);
        verify(proveedores, never()).delete(any());
    }

    /** RN-09: el NIT es unico. */
    @Test
    @DisplayName("RN-09: crear un proveedor con NIT repetido se rechaza")
    void rn09_nitDuplicadoSeRechaza() {

        when(proveedores.existsByNit("900100001-1")).thenReturn(true);

        var req = new ProveedorRequest("Otra empresa", "900100001-1",
                null, null, null, 2);

        assertThrows(BusinessRuleException.class, () -> service.crear(req));
        verify(proveedores, never()).save(any());
    }

    @Test
    @DisplayName("Un proveedor inexistente responde 404")
    void proveedorInexistenteLanza404() {

        when(proveedores.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(99L));
    }
}
