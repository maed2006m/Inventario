import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { Participacion } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent, PALETA } from '../../shared/grafico.component';

/** Participación por proveedor (HU13, RF-26) y concentración HHI. */
@Component({
  selector: 'app-participacion',
  standalone: true,
  imports: [GraficoComponent, FormatoMonedaPipe, DecimalPipe],
  template: `
    @if (datos(); as d) {
      <div class="rejilla-2 arriba">
        <section class="panel">
          <h2>Valor del inventario por proveedor</h2>
          @if (grafico(); as g) {
            <app-grafico [config]="g" [alto]="320" titulo="Participación por proveedor" />
          }
        </section>
        <section class="panel">
          <h2>Concentración de proveedores</h2>
          <div [class]="'hhi ' + d.nivelConcentracion.toLowerCase()">
            <span class="valor">{{ d.indiceHhi | number: '1.0-0' }}</span>
            <span>HHI · concentración {{ d.nivelConcentracion.toLowerCase() }}</span>
          </div>
          <p>{{ lectura[d.nivelConcentracion] }}</p>
          <p>Valor total del inventario: <strong>{{ d.valorTotalInventario | moneda }}</strong></p>
          <p class="ayuda">HHI = suma de las participaciones al cuadrado. Menos de 1.500 es baja, de 1.500 a 2.500 moderada y más de 2.500 alta. Con 5 proveedores parejos el HHI es 2.000.</p>
          <table class="simple">
            <thead><tr><th>Proveedor</th><th class="num">Valor</th><th class="num">%</th></tr></thead>
            <tbody>
              @for (p of d.proveedores; track p.proveedorId) {
                <tr><td>{{ p.proveedorNombre }}</td><td class="num">{{ p.valorInventario | moneda }}</td><td class="num">{{ p.participacionPct }}</td></tr>
              }
            </tbody>
          </table>
        </section>
      </div>
    } @else {
      <p class="vacio">Cargando…</p>
    }
  `,
  styles: [
    `
      .arriba { margin-top: 16px; }
      .hhi { display: flex; align-items: baseline; gap: 10px; padding: 12px; border-radius: 8px; background: var(--verde-claro); }
      .hhi .valor { font-size: 2rem; font-weight: 500; }
      .hhi.moderada { background: var(--ambar-claro); }
      .hhi.alta { background: var(--rojo-claro); }
      table.simple { width: 100%; border-collapse: collapse; }
      table.simple th, table.simple td { padding: 8px 10px; border-bottom: 1px solid var(--borde); text-align: left; }
      table.simple .num { text-align: right; }
    `
  ]
})
export class ParticipacionComponent {
  private api = inject(ReportesService);
  datos = signal<Participacion | null>(null);
  lectura: Record<string, string> = {
    BAJA: 'El inventario está repartido entre varios proveedores: el riesgo de depender de uno es bajo.',
    MODERADA: 'Dependencia moderada: conviene tener un proveedor alterno para los productos que más se venden.',
    ALTA: 'Alta dependencia de pocos proveedores: si uno falla, gran parte del inventario queda en riesgo.'
  };

  grafico = computed<ChartConfiguration | null>(() => {
    const p = this.datos()?.proveedores ?? [];
    if (p.length === 0) {
      return null;
    }
    return {
      type: 'doughnut',
      data: {
        labels: p.map((x) => `${x.proveedorNombre} (${x.participacionPct}%)`),
        datasets: [{ data: p.map((x) => x.valorInventario), backgroundColor: PALETA }]
      },
      options: { plugins: { legend: { position: 'bottom' } } }
    };
  });

  constructor() {
    this.api.participacion().subscribe((c) => this.datos.set(c));
  }
}
