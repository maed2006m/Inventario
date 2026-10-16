import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { InventarioService } from '../../../core/inventario.service';
import { Movimiento, Pagina, Producto } from '../../../core/models';
import { BadgeEstadoComponent } from '../../../shared/badge-estado.component';
import { ETIQUETA_MOTIVO } from '../../../shared/etiquetas';
import { FormatoMonedaPipe } from '../../../shared/formato-moneda.pipe';

/** Ficha de un producto en modo lectura con sus movimientos (RF-11, RF-12; sección 6.4 del informe). */
@Component({
  selector: 'app-producto-detalle',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatPaginatorModule, BadgeEstadoComponent, FormatoMonedaPipe, DatePipe],
  template: `
    <h2 mat-dialog-title>{{ p.nombre }}</h2>
    <mat-dialog-content>
      <dl class="ficha">
        <dt>SKU</dt><dd>{{ p.sku }}</dd>
        <dt>Proveedor</dt><dd>{{ p.proveedorNombre }}</dd>
        <dt>Existencia</dt><dd>{{ p.existencia }} unidades <app-badge-estado [estado]="p.estado" /></dd>
        <dt>Stock mínimo</dt><dd>{{ p.stockMinimo }}</dd>
        <dt>Costo / Precio</dt><dd>{{ p.costoUnitario | moneda }} / {{ p.precioVenta | moneda }}</dd>
        <dt>Estado</dt><dd>{{ p.activo ? 'Habilitado' : 'Deshabilitado' }}</dd>
      </dl>
      <h3>Movimientos</h3>
      @if (pagina(); as pg) {
        <table class="mini">
          <thead><tr><th>Fecha</th><th>Motivo</th><th class="num">Cantidad</th><th class="ocultar-movil">Usuario</th></tr></thead>
          <tbody>
            @for (m of pg.content; track m.id) {
              <tr>
                <td>{{ m.fecha | date: 'dd/MM/yy HH:mm' }}</td>
                <td>{{ etiqueta[m.motivo] }}</td>
                <td class="num" [class.salida]="m.tipo === 'SALIDA'">{{ m.tipo === 'SALIDA' ? '−' : '+' }}{{ m.cantidad }}</td>
                <td class="ocultar-movil">{{ m.usuario }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="vacio">Sin movimientos</td></tr>
            }
          </tbody>
        </table>
        <mat-paginator [length]="pg.totalElements" [pageSize]="10" [hidePageSize]="true" (page)="cambiar($event)" />
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cerrar</button></mat-dialog-actions>
  `,
  styles: [
    `
      .ficha { display: grid; grid-template-columns: max-content 1fr; gap: 6px 16px; margin: 0 0 12px; }
      dt { color: var(--gris); }
      dd { margin: 0; display: flex; gap: 8px; align-items: center; }
      table.mini { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
      table.mini th, table.mini td { padding: 6px 8px; border-bottom: 1px solid var(--borde); text-align: left; }
      .num { text-align: right !important; }
      .salida { color: var(--rojo); }
      h3 { font-weight: 500; font-size: 1rem; }
    `
  ]
})
export class ProductoDetalleComponent {
  p = inject<Producto>(MAT_DIALOG_DATA);
  private api = inject(InventarioService);
  pagina = signal<Pagina<Movimiento> | null>(null);
  etiqueta = ETIQUETA_MOTIVO;

  constructor() {
    this.cargar(0);
  }

  cambiar(e: PageEvent): void {
    this.cargar(e.pageIndex);
  }

  private cargar(page: number): void {
    this.api.movimientosDeProducto(this.p.id, page, 10).subscribe((r) => this.pagina.set(r));
  }
}
