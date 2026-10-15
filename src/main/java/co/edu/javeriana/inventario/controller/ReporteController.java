package co.edu.javeriana.inventario.controller;

import co.edu.javeriana.inventario.dto.*;
import co.edu.javeriana.inventario.model.*;
import co.edu.javeriana.inventario.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reportes;
    private final DashboardService dashboard;
    private final ReporteCsvService csv;

    @GetMapping("/reportes/rotacion")
    public List<RotacionResponse> rotacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate ini = desde != null ? desde : fin.minusDays(29);
        return reportes.rotacion(ini, fin);
    }

    @GetMapping("/reportes/participacion")
    public ParticipacionResponse participacion() {
        return reportes.participacion();
    }

    @GetMapping("/dashboard/resumen")
    public DashboardResponse resumen() {
        return dashboard.resumen();
    }

    @GetMapping("/movimientos/export")
    public ResponseEntity<StreamingResponseBody> exportar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) MotivoMovimiento motivo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        csv.validar(desde, hasta);   // antes de transmitir, para que un error salga como 400
        StreamingResponseBody cuerpo = salida -> csv.escribir(salida, productoId, tipo, motivo, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=movimientos.csv")
                .contentType(new MediaType("text", "csv"))
                .body(cuerpo);
    }
}