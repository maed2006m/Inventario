import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ChartConfiguration } from 'chart.js';
import { errorDeApi } from '../../core/errores';
import { Participacion } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent, PALETA } from '../../shared/grafico.component';

/** Tope visual del medidor: por encima de este HHI el marcador queda al final. */
const TOPE_MEDIDOR = 5000;

/**
 * Participación por proveedor (HU13, RF-26) y concentración HHI.
 * El HHI y el nivel de concentración llegan calculados del backend; aquí solo se dibujan.
 */
@Component({
  selector: 'app-participacion',
  standalone: true,
  imports: [GraficoComponent, FormatoMonedaPipe, DecimalPipe, MatIconModule],
  templateUrl: './participacion.component.html',
  styleUrl: './participacion.component.css'
})
export class ParticipacionComponent {
  private api = inject(ReportesService);

  readonly paleta = PALETA;
  datos = signal<Participacion | null>(null);
  error = signal<{ message: string; accion: string } | null>(null);

  lectura: Record<string, string> = {
    BAJA: 'El inventario está repartido entre varios proveedores: el riesgo de depender de uno es bajo.',
    MODERADA: 'Dependencia moderada: conviene tener un proveedor alterno para los productos que más se venden.',
    ALTA: 'Alta dependencia de pocos proveedores: si uno falla, gran parte del inventario queda en riesgo.'
  };
  /** Color de la píldora según el nivel de concentración. */
  claseNivel: Record<string, string> = { BAJA: 'verde', MODERADA: 'ambar', ALTA: 'rojo' };

  /** Proveedor con mayor participación (la API los entrega de mayor a menor valor). */
  principal = computed(() => {
    const p = this.datos()?.proveedores ?? [];
    return p.length ? p.reduce((a, b) => (b.participacionPct > a.participacionPct ? b : a)) : null;
  });

  /** Posición del marcador del medidor, de 0 a 100. */
  marcador = computed(() => Math.min(100, ((this.datos()?.indiceHhi ?? 0) / TOPE_MEDIDOR) * 100));

  grafico = computed<ChartConfiguration | null>(() => {
    const p = this.datos()?.proveedores ?? [];
    if (p.length === 0) {
      return null;
    }
    return {
      type: 'doughnut',
      data: {
        labels: p.map((x) => x.proveedorNombre),
        datasets: [{ data: p.map((x) => x.valorInventario), backgroundColor: PALETA, borderWidth: 0, hoverOffset: 8 }]
      },
      // La leyenda se dibuja en HTML junto al gráfico.
      options: { cutout: '66%', plugins: { legend: { display: false } } }
    } as ChartConfiguration;
  });

  constructor() {
    this.api.participacion().subscribe({
      next: (c) => this.datos.set(c),
      error: (e) => this.error.set(errorDeApi(e))
    });
  }
}
