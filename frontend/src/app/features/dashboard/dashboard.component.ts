import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { AuthService } from '../../core/auth.service';
import { InventarioService } from '../../core/inventario.service';
import { Dashboard, ParticipacionFila, Producto } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { BadgeEstadoComponent } from '../../shared/badge-estado.component';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent, PALETA } from '../../shared/grafico.component';

/** Tablero principal con KPIs y productos críticos (Ilustración 3, RF-23). */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, BadgeEstadoComponent, FormatoMonedaPipe, GraficoComponent],
  template: `
    <div class="pagina">
      <div class="barra-acciones">
        <h1>Hola, {{ auth.sesion()?.nombre }}</h1>
        <span class="espacio"></span>
        <a mat-flat-button color="primary" routerLink="/movimientos/nuevo"><mat-icon>add</mat-icon> Registrar movimiento</a>
      </div>
      @if (datos(); as d) {
        <div class="tarjetas">
          <div class="tarjeta-kpi"><div class="etiqueta">Productos activos</div><div class="valor">{{ d.productosActivos }}</div></div>
          <div class="tarjeta-kpi alerta"><div class="etiqueta">Bajo stock</div><div class="valor">{{ d.productosBajoStock }}</div></div>
          <div class="tarjeta-kpi peligro"><div class="etiqueta">Agotados</div><div class="valor">{{ d.productosAgotados }}</div></div>
          <div class="tarjeta-kpi"><div class="etiqueta">Movimientos hoy</div><div class="valor">{{ d.movimientosHoy }}</div></div>
          <div class="tarjeta-kpi"><div class="etiqueta">Valor del inventario</div><div class="valor">{{ d.valorInventario | moneda }}</div></div>
          <div class="tarjeta-kpi"><div class="etiqueta">Ventas del mes</div><div class="valor">{{ d.ventasDelMes | moneda }}</div></div>
        </div>
        <div class="rejilla-2">
          <section class="panel">
            <h2>Productos críticos</h2>
            @if (criticos().length === 0) {
              <p class="vacio">Todo el inventario está por encima del mínimo.</p>
            } @else {
              <div class="tabla-contenedor">
                <table class="simple">
                  <thead><tr><th>Producto</th><th class="num">Existencia</th><th class="num ocultar-movil">Mínimo</th><th>Estado</th></tr></thead>
                  <tbody>
                    @for (p of criticos(); track p.id) {
                      <tr>
                        <td>{{ p.nombre }}</td>
                        <td class="num">{{ p.existencia }}</td>
                        <td class="num ocultar-movil">{{ p.stockMinimo }}</td>
                        <td><app-badge-estado [estado]="p.estado" /></td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
              <a mat-button routerLink="/alertas">Ver todas las alertas</a>
            }
          </section>
          @if (auth.esAdmin()) {
            @if (graficoProveedores(); as g) {
              <section class="panel">
                <h2>Valor del inventario por proveedor</h2>
                <app-grafico [config]="g" titulo="Participación por proveedor" />
              </section>
            }
          }
        </div>
      } @else if (error()) {
        <div class="error-servidor">{{ error() }}</div>
      } @else {
        <p class="vacio">Cargando…</p>
      }
    </div>
  `,
  styles: [
    `
      table.simple { border-collapse: collapse; }
      table.simple th, table.simple td { padding: 10px 12px; border-bottom: 1px solid var(--borde); text-align: left; }
      table.simple th.num, table.simple td.num { text-align: right; }
    `
  ]
})
export class DashboardComponent {
  auth = inject(AuthService);
  private api = inject(InventarioService);
  private reportes = inject(ReportesService);

  datos = signal<Dashboard | null>(null);
  error = signal<string | null>(null);
  /** Los 5 productos más urgentes (agotados primero), tomados de /api/productos/alertas. */
  criticos = signal<Producto[]>([]);
  private participacion = signal<ParticipacionFila[]>([]);

  graficoProveedores = computed<ChartConfiguration | null>(() => {
    const p = this.participacion();
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
    this.api.dashboard().subscribe({
      next: (d) => this.datos.set(d),
      error: () => this.error.set('No se pudo cargar el tablero. Verifique que el backend esté en ejecución.')
    });
    this.api.alertas().subscribe((a) => this.criticos.set(a.slice(0, 5)));
    if (this.auth.esAdmin()) {
      this.reportes.participacion().subscribe((p) => this.participacion.set(p.proveedores));
    }
  }
}
