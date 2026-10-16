import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ChartConfiguration } from 'chart.js';
import { errorDeApi } from '../../core/errores';
import { RotacionItem } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { fechaIso } from '../../shared/etiquetas';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent } from '../../shared/grafico.component';

/** Ranking de rotación con selector de periodo (HU12, RF-25). */
@Component({
  selector: 'app-rotacion',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, GraficoComponent, FormatoMonedaPipe],
  template: `
    <div class="barra-acciones periodo">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Desde</mat-label>
        <input matInput type="date" [(ngModel)]="desde" />
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Hasta</mat-label>
        <input matInput type="date" [(ngModel)]="hasta" />
      </mat-form-field>
      <button mat-flat-button color="primary" (click)="cargar()">Consultar</button>
    </div>
    @if (error(); as e) {
      <div class="error-servidor"><strong>{{ e.message }}</strong>{{ e.accion }}</div>
    }
    <div class="rejilla-2">
      <section class="panel">
        <h2>Unidades vendidas (top 10)</h2>
        @if (grafico(); as g) {
          <app-grafico [config]="g" [alto]="340" titulo="Ranking de rotación" />
        }
      </section>
      <section class="panel">
        <h2>Ranking completo</h2>
        <div class="tabla-contenedor">
          <table class="simple">
            <thead><tr><th>#</th><th>Producto</th><th class="num">Unidades</th><th class="num ocultar-movil">Ingresos</th><th class="num">Rotación</th></tr></thead>
            <tbody>
              @for (r of filas(); track r.productoId) {
                <tr [class.quieto]="r.unidadesVendidas === 0">
                  <td>{{ r.posicion }}</td>
                  <td>{{ r.productoNombre }}<div class="ayuda">{{ r.proveedorNombre }}</div></td>
                  <td class="num">{{ r.unidadesVendidas }}</td>
                  <td class="num ocultar-movil">{{ r.ingresos | moneda }}</td>
                  <td class="num">{{ r.indiceRotacion ?? '—' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="ayuda">Rotación = unidades vendidas / existencia promedio del periodo. En gris, lo que no se vendió.</p>
      </section>
    </div>
  `,
  styles: [
    `
      .periodo { margin-top: 16px; }
      table.simple { width: 100%; border-collapse: collapse; }
      table.simple th, table.simple td { padding: 8px 10px; border-bottom: 1px solid var(--borde); text-align: left; }
      table.simple .num { text-align: right; }
      .quieto { color: var(--gris); }
    `
  ]
})
export class RotacionComponent {
  private api = inject(ReportesService);
  filas = signal<RotacionItem[]>([]);
  error = signal<{ message: string; accion: string } | null>(null);
  hasta = fechaIso(new Date());
  desde = fechaIso(new Date(Date.now() - 29 * 86_400_000));

  grafico = computed<ChartConfiguration | null>(() => {
    const top = this.filas().slice(0, 10);
    if (top.length === 0) {
      return null;
    }
    return {
      type: 'bar',
      data: {
        labels: top.map((r) => r.productoNombre),
        datasets: [{ label: 'Unidades vendidas', data: top.map((r) => r.unidadesVendidas), backgroundColor: '#1f6f5c' }]
      },
      options: { indexAxis: 'y', plugins: { legend: { display: false } } }
    };
  });

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.api.rotacion(this.desde, this.hasta).subscribe({
      next: (r) => this.filas.set(r),
      error: (e) => this.error.set(errorDeApi(e))
    });
  }
}
