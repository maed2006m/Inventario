import { DatePipe } from '@angular/common';
import { HttpEventType } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { AuthService } from '../../core/auth.service';
import { InventarioService } from '../../core/inventario.service';
import { Dashboard, Movimiento, ParticipacionFila, Producto, RotacionItem } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { CLASE_MOTIVO, ETIQUETA_MOTIVO } from '../../shared/etiquetas';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent, PALETA } from '../../shared/grafico.component';

/** Días que cubren el flujo, la rotación y la exportación del tablero. */
const DIAS_PERIODO = 30;

/**
 * Tablero principal en cuadrícula "bento" (Ilustración 3, RF-23).
 *
 * Todas las tarjetas muestran datos reales de la API; no se calcula ninguna regla de
 * negocio aquí. Rotación, participación y exportación son reportes del rol ADMIN
 * (RNF-10): al Tendero se le muestra un aviso en lugar de pedirlos y recibir un 403.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, MatIconModule, MatSnackBarModule, FormatoMonedaPipe, GraficoComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  auth = inject(AuthService);
  private api = inject(InventarioService);
  private reportes = inject(ReportesService);
  private snack = inject(MatSnackBar);

  readonly dias = DIAS_PERIODO;
  readonly etiquetaMotivo = ETIQUETA_MOTIVO;
  readonly claseMotivo = CLASE_MOTIVO;
  readonly paleta = PALETA;

  datos = signal<Dashboard | null>(null);
  error = signal<string | null>(null);

  /** Productos bajo el mínimo (agotados primero), de /api/productos/alertas. */
  alertas = signal<Producto[]>([]);
  /** Las 5 alertas más urgentes que caben en la tarjeta. */
  criticos = computed(() => this.alertas().slice(0, 5));

  /** Cifras del flujo proveedores → productos → movimientos. */
  proveedoresActivos = signal<number | null>(null);
  entradas = signal<number | null>(null);
  salidas = signal<number | null>(null);

  ultimos = signal<Movimiento[]>([]);
  rotacion = signal<RotacionItem[]>([]);
  participacion = signal<ParticipacionFila[]>([]);
  exportando = signal(false);

  /** Indican que cada tarjeta ya recibió respuesta; mientras tanto se muestra su esqueleto. */
  alertasListas = signal(false);
  ultimosListos = signal(false);
  rotacionLista = signal(false);
  participacionLista = signal(false);
  /** Filas de relleno de los esqueletos de carga. */
  readonly relleno = [1, 2, 3, 4];

  /** Los 5 productos con más salida y el máximo para escalar las barras. */
  topRotacion = computed(() => this.rotacion().slice(0, 5));
  maxRotacion = computed(() => Math.max(1, ...this.topRotacion().map((r) => r.unidadesVendidas)));

  graficoProveedores = computed<ChartConfiguration | null>(() => {
    const p = this.participacion();
    if (p.length === 0) {
      return null;
    }
    return {
      type: 'doughnut',
      data: {
        labels: p.map((x) => x.proveedorNombre),
        datasets: [{ data: p.map((x) => x.valorInventario), backgroundColor: PALETA, borderWidth: 0, hoverOffset: 6 }]
      },
      // La leyenda se dibuja en HTML dentro de la tarjeta, no en el lienzo.
      options: { cutout: '68%', plugins: { legend: { display: false } } }
    } as ChartConfiguration;
  });

  constructor() {
    const desde = this.inicioPeriodo();

    this.api.dashboard().subscribe({
      next: (d) => this.datos.set(d),
      error: () => this.error.set('No se pudo cargar el tablero. Verifique que el backend esté en ejecución.')
    });
    this.api.alertas().subscribe({
      next: (a) => this.alertas.set(a),
      error: () => this.alertasListas.set(true),
      complete: () => this.alertasListas.set(true)
    });
    this.api.proveedores(true).subscribe((p) => this.proveedoresActivos.set(p.length));
    this.api.historial({}, 0, 6).subscribe({
      next: (p) => this.ultimos.set(p.content),
      error: () => this.ultimosListos.set(true),
      complete: () => this.ultimosListos.set(true)
    });
    // Solo se necesita el total de cada tipo, así que se pide una página de un elemento.
    this.api.historial({ tipo: 'ENTRADA', desde }, 0, 1).subscribe((p) => this.entradas.set(p.totalElements));
    this.api.historial({ tipo: 'SALIDA', desde }, 0, 1).subscribe((p) => this.salidas.set(p.totalElements));

    if (this.auth.esAdmin()) {
      this.reportes.rotacion().subscribe({
        next: (r) => this.rotacion.set(r),
        error: () => this.rotacionLista.set(true),
        complete: () => this.rotacionLista.set(true)
      });
      this.reportes.participacion().subscribe({
        next: (p) => this.participacion.set(p.proveedores),
        error: () => this.participacionLista.set(true),
        complete: () => this.participacionLista.set(true)
      });
    }
  }

  /** Porcentaje de la existencia frente al mínimo, para la barra de cada alerta. */
  nivel(p: Producto): number {
    return p.stockMinimo > 0 ? Math.min(100, Math.round((100 * p.existencia) / p.stockMinimo)) : 0;
  }

  /** Ancho de la barra de rotación respecto al producto más vendido. */
  ancho(r: RotacionItem): number {
    return Math.round((100 * r.unidadesVendidas) / this.maxRotacion());
  }

  /** Descarga el CSV de los movimientos del periodo (RNF-07). Solo ADMIN. */
  exportarCsv(): void {
    this.exportando.set(true);
    this.api.exportarCsv({ desde: this.inicioPeriodo() }).subscribe({
      next: (ev) => {
        if (ev.type === HttpEventType.Response) {
          const a = document.createElement('a');
          a.href = URL.createObjectURL(ev.body as Blob);
          a.download = 'movimientos.csv';
          a.click();
          URL.revokeObjectURL(a.href);
          this.exportando.set(false);
        }
      },
      error: () => {
        this.exportando.set(false);
        this.snack.open('No se pudo generar el CSV', 'OK');
      }
    });
  }

  /** Inicio del periodo en ISO 8601, el formato que espera el filtro "desde". */
  private inicioPeriodo(): string {
    const d = new Date();
    d.setDate(d.getDate() - DIAS_PERIODO);
    d.setHours(0, 0, 0, 0);
    return d.toISOString();
  }
}
