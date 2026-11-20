package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.exception.SolicitudInvalidaException;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.Producto;
import co.edu.javeriana.inventario.model.Proveedor;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.model.Usuario;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exportacion CSV del historial (HU15).
 *
 * Lo critico aqui es el escape de celdas: el separador, las comillas y los
 * saltos de linea no deben romper las columnas, y un texto que empieza por
 * =, +, - o @ no debe ejecutarse como formula al abrir el archivo en Excel
 * (inyeccion CSV).
 */
@ExtendWith(MockitoExtension.class)
class ReporteCsvServiceTest {

    @Mock MovimientoRepository movimientos;

    @InjectMocks ReporteCsvService service;

    private static Movimiento movimiento(long id, String observacion) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora; Andina");

        Producto producto = new Producto();
        producto.setSku("ARZ-500");
        producto.setNombre("Arroz \"Diana\" 500g");
        producto.setProveedor(proveedor);

        Usuario usuario = new Usuario();
        usuario.setUsername("tendero");

        Movimiento m = new Movimiento();
        m.setId(id);
        m.setProducto(producto);
        m.setTipo(TipoMovimiento.SALIDA);
        m.setMotivo(MotivoMovimiento.VENTA);
        m.setCantidad(3);
        m.setValorUnitario(new BigDecimal("1500.5"));
        m.setObservacion(observacion);
        m.setUsuario(usuario);
        // 15:30 UTC son las 10:30 en Bogota
        m.setFecha(OffsetDateTime.of(2026, 6, 1, 15, 30, 0, 0, ZoneOffset.UTC));
        return m;
    }

    // ---------- escape de celdas ----------

    @Test
    @DisplayName("HU15: una celda nula o vacia queda vacia y un texto simple no se altera")
    void celdaSimple() {
        assertEquals("", ReporteCsvService.celda(null));
        assertEquals("", ReporteCsvService.celda(""));
        assertEquals("Arroz 500g", ReporteCsvService.celda("Arroz 500g"));
    }

    @Test
    @DisplayName("HU15: un texto que empieza como formula se neutraliza con un apostrofo")
    void celdaConFormula() {
        assertEquals("'=SUMA(A1:A9)", ReporteCsvService.celda("=SUMA(A1:A9)"));
        assertEquals("'+57 300", ReporteCsvService.celda("+57 300"));
        assertEquals("'-5", ReporteCsvService.celda("-5"));
        assertEquals("'@cmd", ReporteCsvService.celda("@cmd"));
        assertEquals("'\tx", ReporteCsvService.celda("\tx"));
    }

    @Test
    @DisplayName("HU15: separador, comillas y saltos de linea se encierran entre comillas")
    void celdaConCaracteresEspeciales() {
        assertEquals("\"a;b\"", ReporteCsvService.celda("a;b"));
        assertEquals("\"dijo \"\"hola\"\"\"", ReporteCsvService.celda("dijo \"hola\""));
        assertEquals("\"linea1\nlinea2\"", ReporteCsvService.celda("linea1\nlinea2"));
        assertEquals("\"fin\r\"", ReporteCsvService.celda("fin\r"));
        // Formula y separador a la vez: primero el apostrofo, luego las comillas
        assertEquals("\"'=1;2\"", ReporteCsvService.celda("=1;2"));
        // Un \r al inicio cuenta como formula y ademas exige comillas
        assertEquals("\"'\rx\"", ReporteCsvService.celda("\rx"));
    }

    @Test
    @DisplayName("HU15: los numeros salen con dos decimales y coma decimal")
    void numeros() {
        assertEquals("1500,50", ReporteCsvService.numero(new BigDecimal("1500.5")));
        assertEquals("0,00", ReporteCsvService.numero(BigDecimal.ZERO));
        assertEquals("2,35", ReporteCsvService.numero(new BigDecimal("2.345")));
    }

    @Test
    @DisplayName("HU15: la fila trae fecha en hora de Bogota, total calculado y celdas escapadas")
    void filaCompleta() {
        String fila = ReporteCsvService.fila(movimiento(7, "=HACK()"));

        assertEquals("7;2026-06-01 10:30:00;ARZ-500;\"Arroz \"\"Diana\"\" 500g\";\"Distribuidora; Andina\";"
                + "SALIDA;VENTA;3;1500,50;4501,50;tendero;'=HACK()", fila);
    }

    // ---------- validacion y escritura ----------

    @Test
    @DisplayName("RN-15: fechas invertidas se rechazan; nulas o en orden se aceptan")
    void validarFechas() {
        OffsetDateTime antes = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime despues = antes.plusDays(1);

        assertDoesNotThrow(() -> service.validar(null, null));
        assertDoesNotThrow(() -> service.validar(antes, null));
        assertDoesNotThrow(() -> service.validar(null, despues));
        assertDoesNotThrow(() -> service.validar(antes, despues));

        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> service.validar(despues, antes));
        assertNotNull(e.getAccion());
    }

    /**
     * El archivo se escribe por lotes para no cargar todo el historial en
     * memoria. Con dos paginas, el servicio debe pedir ambas y escribir las
     * filas de las dos, despues del BOM y el encabezado.
     */
    @Test
    @DisplayName("HU15: el CSV se escribe por lotes con BOM, encabezado y una fila por movimiento")
    @SuppressWarnings("unchecked")
    void escribirPorLotes() throws Exception {
        when(movimientos.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento(1, null)), PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(movimiento(2, "ok")), PageRequest.of(1, 1), 2));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        service.escribir(salida, null, null, null, null, null, null);

        String csv = salida.toString(StandardCharsets.UTF_8);
        String[] lineas = csv.split("\r\n");

        assertEquals('﻿', csv.charAt(0), "Debe empezar con BOM para que Excel lea UTF-8");
        assertEquals(3, lineas.length, "Encabezado + dos filas");
        assertTrue(lineas[0].endsWith("ID;Fecha;SKU;Producto;Proveedor;Tipo;Motivo;Cantidad;"
                + "Valor unitario;Valor total;Usuario;Observacion"));
        assertTrue(lineas[1].startsWith("1;"));
        assertTrue(lineas[1].endsWith(";tendero;"), "Observacion nula queda vacia");
        assertTrue(lineas[2].startsWith("2;"));
        verify(movimientos, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }
}
