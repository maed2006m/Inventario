package co.edu.javeriana.inventario.service;

import co.edu.javeriana.inventario.exception.SolicitudInvalidaException;
import co.edu.javeriana.inventario.model.MotivoMovimiento;
import co.edu.javeriana.inventario.model.Movimiento;
import co.edu.javeriana.inventario.model.TipoMovimiento;
import co.edu.javeriana.inventario.repository.MovimientoRepository;
import co.edu.javeriana.inventario.repository.MovimientoSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exportación del historial de movimientos a CSV (RF-29, HU15, RNF-07).
 *
 * Formato pensado para Excel con configuración regional de Colombia:
 * separador de columnas ";", decimales con coma, codificación UTF-8 con BOM.
 * Escribe por lotes para no cargar todo el historial en memoria.
 */
@Service
@RequiredArgsConstructor
public class ReporteCsvService {

    /** Cuántos movimientos se leen de la base por cada lote. */
    static final int TAMANIO_LOTE = 500;

    static final String SEPARADOR = ";";
    static final String SALTO = "\r\n";

    private static final String ENCABEZADO = String.join(SEPARADOR,
            "ID", "Fecha", "SKU", "Producto", "Proveedor", "Tipo", "Motivo",
            "Cantidad", "Valor unitario", "Valor total", "Usuario", "Observacion");

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MovimientoRepository movimientos;

    /**
     * Valida el rango de fechas. El controlador debe llamarlo ANTES de empezar a transmitir el archivo:
     * un error durante la transmisión llegaría cuando la respuesta ya empezó y no se vería como un 400.
     */
    public void validar(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    "La fecha inicial (" + desde + ") es posterior a la fecha final (" + hasta + ").",
                    "Corrija el rango: la fecha inicial debe ser anterior o igual a la final.");
        }
    }

    /**
     * Escribe el CSV completo en el flujo de salida, con los mismos filtros del historial.
     * Los filtros en null se ignoran. Orden: del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public void escribir(OutputStream salida, Long productoId, TipoMovimiento tipo, MotivoMovimiento motivo,
                         String username, OffsetDateTime desde, OffsetDateTime hasta) throws IOException {
        validar(desde, hasta);
        Specification<Movimiento> filtro = MovimientoSpecs.filtrar(productoId, tipo, motivo, username, desde, hasta);
        Sort orden = Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id"));

        Writer escritor = new BufferedWriter(new OutputStreamWriter(salida, StandardCharsets.UTF_8));
        escritor.write('\uFEFF');                       // BOM: Excel reconoce UTF-8 y muestra bien las tildes
        escritor.write(ENCABEZADO + SALTO);

        int pagina = 0;
        Page<Movimiento> lote;
        do {
            lote = movimientos.findAll(filtro, PageRequest.of(pagina++, TAMANIO_LOTE, orden));
            for (Movimiento m : lote.getContent()) {
                escritor.write(fila(m) + SALTO);
            }
            escritor.flush();                           // el navegador va recibiendo el archivo mientras se genera
        } while (lote.hasNext());
    }

    /** Convierte un movimiento en una línea del CSV. Visible en el paquete para poder probarlo. */
    static String fila(Movimiento m) {
        BigDecimal total = m.getValorUnitario().multiply(BigDecimal.valueOf(m.getCantidad()));
        return String.join(SEPARADOR,
                String.valueOf(m.getId()),
                m.getFecha().atZoneSameInstant(ReporteService.ZONA).format(FORMATO_FECHA),
                celda(m.getProducto().getSku()),
                celda(m.getProducto().getNombre()),
                celda(m.getProducto().getProveedor().getNombre()),
                m.getTipo().name(),
                m.getMotivo().name(),
                String.valueOf(m.getCantidad()),
                numero(m.getValorUnitario()),
                numero(total),
                celda(m.getUsuario().getUsername()),
                celda(m.getObservacion()));
    }

    /** Número con 2 decimales y coma decimal, como lo espera Excel en Colombia. Sin separador de miles. */
    static String numero(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Prepara un texto para una celda del CSV.
     * 1) Protege contra la inyección de fórmulas: si el texto empieza con =, +, - o @,
     *    Excel lo ejecutaría como fórmula; se le antepone un apóstrofo para que sea texto.
     * 2) Si contiene ";", comillas o saltos de línea, lo encierra entre comillas y duplica las comillas internas.
     */
    static String celda(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        String texto = valor;
        if ("=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        boolean necesitaComillas = texto.contains(SEPARADOR) || texto.contains("\"")
                || texto.contains("\n") || texto.contains("\r");
        if (necesitaComillas) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}