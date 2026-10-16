import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { InventarioService } from '../../../core/inventario.service';
import { Producto } from '../../../core/models';
import { BadgeEstadoComponent } from '../../../shared/badge-estado.component';

/** Alertas de stock (Ilustración 6, HU11): agotados primero y luego los más cercanos al mínimo. */
@Component({
  selector: 'app-alertas',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, BadgeEstadoComponent],
  template: `
    <div class="pagina">
      <h1>Alertas de stock</h1>
      @if (alertas(); as lista) {
        @if (lista.length === 0) {
          <p class="vacio panel">No hay productos en bajo stock ni agotados.</p>
        } @else {
          <p class="ayuda">{{ lista.length }} productos necesitan reposición.</p>
          <div class="lista">
            @for (p of lista; track p.id) {
              <article class="panel tarjeta" [class.agotado]="p.estado === 'AGOTADO'">
                <div class="cabeza">
                  <strong>{{ p.nombre }}</strong>
                  <app-badge-estado [estado]="p.estado" />
                </div>
                <div class="ayuda">{{ p.proveedorNombre }} · SKU {{ p.sku }}</div>
                <div class="cifras">
                  <span>Existencia <strong>{{ p.existencia }}</strong></span>
                  <span>Mínimo <strong>{{ p.stockMinimo }}</strong></span>
                </div>
                <a mat-stroked-button [routerLink]="['/movimientos/nuevo']" [queryParams]="{ productoId: p.id }">
                  <mat-icon>add_shopping_cart</mat-icon> Registrar compra
                </a>
              </article>
            }
          </div>
        }
      } @else {
        <p class="vacio">Cargando…</p>
      }
    </div>
  `,
  styles: [
    `
      .lista { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
      .tarjeta { margin: 0; display: grid; gap: 8px; border-left: 4px solid var(--ambar); }
      .tarjeta.agotado { border-left-color: var(--rojo); }
      .cabeza { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
      .cifras { display: flex; gap: 16px; }
    `
  ]
})
export class AlertasComponent {
  private api = inject(InventarioService);
  alertas = signal<Producto[] | null>(null);

  constructor() {
    this.api.alertas().subscribe((a) => this.alertas.set(a));
  }
}
