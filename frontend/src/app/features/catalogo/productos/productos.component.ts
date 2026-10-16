import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { errorDeApi } from '../../../core/errores';
import { InventarioService } from '../../../core/inventario.service';
import { EstadoStock, Producto, Proveedor } from '../../../core/models';
import { BadgeEstadoComponent } from '../../../shared/badge-estado.component';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { FormatoMonedaPipe } from '../../../shared/formato-moneda.pipe';
import { ProductoDetalleComponent } from './producto-detalle.component';
import { ProductoDialogComponent } from './producto-dialog.component';

/** Catálogo de productos con existencia y estado calculados (Ilustración 4, HU4 a HU6, HU10, RF-24). */
@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    BadgeEstadoComponent,
    FormatoMonedaPipe
  ],
  template: `
    <div class="pagina">
      <h1>Productos</h1>
      <div class="barra-acciones">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Buscar por nombre o SKU</mat-label>
          <input matInput [(ngModel)]="q" (ngModelChange)="cargar()" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Proveedor</mat-label>
          <mat-select [(ngModel)]="proveedorId" (selectionChange)="cargar()">
            <mat-option [value]="null">Todos</mat-option>
            @for (p of proveedores(); track p.id) {
              <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Categoría</mat-label>
          <mat-select [(ngModel)]="categoria" (selectionChange)="cargar()">
            <mat-option [value]="null">Todas</mat-option>
            @for (c of categorias(); track c) {
              <mat-option [value]="c">{{ c }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Estado</mat-label>
          <mat-select [(ngModel)]="estado" (selectionChange)="cargar()">
            <mat-option [value]="null">Todos</mat-option>
            <mat-option value="EN_STOCK">En stock</mat-option>
            <mat-option value="BAJO_STOCK">Bajo stock</mat-option>
            <mat-option value="AGOTADO">Agotado</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-slide-toggle [(ngModel)]="soloActivos" (change)="cargar()">Solo habilitados</mat-slide-toggle>
        <span class="espacio"></span>
        <button mat-flat-button color="primary" (click)="abrir(null)"><mat-icon>add</mat-icon> Nuevo producto</button>
      </div>

      <div class="tabla-contenedor">
        <table mat-table [dataSource]="productos()">
          <ng-container matColumnDef="sku">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">SKU</th>
            <td mat-cell *matCellDef="let p" class="ocultar-movil">{{ p.sku }}</td>
          </ng-container>
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Producto</th>
            <td mat-cell *matCellDef="let p">
              <button class="enlace" (click)="ver(p)">{{ p.nombre }}</button>
              @if (!p.activo) { <span class="ayuda"> (deshabilitado)</span> }
            </td>
          </ng-container>
          <ng-container matColumnDef="proveedor">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Proveedor</th>
            <td mat-cell *matCellDef="let p" class="ocultar-movil">{{ p.proveedorNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="existencia">
            <th mat-header-cell *matHeaderCellDef class="num">Existencia</th>
            <td mat-cell *matCellDef="let p" class="num">{{ p.existencia }}</td>
          </ng-container>
          <ng-container matColumnDef="minimo">
            <th mat-header-cell *matHeaderCellDef class="num ocultar-movil">Mínimo</th>
            <td mat-cell *matCellDef="let p" class="num ocultar-movil">{{ p.stockMinimo }}</td>
          </ng-container>
          <ng-container matColumnDef="precio">
            <th mat-header-cell *matHeaderCellDef class="num ocultar-movil">Precio</th>
            <td mat-cell *matCellDef="let p" class="num ocultar-movil">{{ p.precioVenta | moneda }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let p"><app-badge-estado [estado]="p.estado" /></td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let p" class="num">
              <button mat-icon-button (click)="abrir(p)" matTooltip="Editar" aria-label="Editar"><mat-icon>edit</mat-icon></button>
              @if (p.activo) {
                <button mat-icon-button (click)="deshabilitar(p)" matTooltip="Deshabilitar" aria-label="Deshabilitar">
                  <mat-icon>visibility_off</mat-icon>
                </button>
              } @else {
                <button mat-icon-button (click)="habilitar(p)" matTooltip="Habilitar" aria-label="Habilitar">
                  <mat-icon>visibility</mat-icon>
                </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let row; columns: columnas"></tr>
        </table>
        @if (productos().length === 0) {
          <p class="vacio">No hay productos que coincidan con los filtros.</p>
        }
      </div>
      <p class="ayuda">{{ productos().length }} productos · la existencia se calcula con entradas menos salidas.</p>
    </div>
  `,
  styles: [`.enlace { background: none; border: 0; padding: 0; color: #1a56a8; cursor: pointer; font: inherit; text-align: left; }`]
})
export class ProductosComponent {
  private api = inject(InventarioService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  productos = signal<Producto[]>([]);
  proveedores = signal<Proveedor[]>([]);
  private todasCategorias = signal<string[]>([]);
  categorias = computed(() => this.todasCategorias());

  q = '';
  proveedorId: number | null = null;
  categoria: string | null = null;
  estado: EstadoStock | null = null;
  soloActivos = true;
  columnas = ['sku', 'nombre', 'proveedor', 'existencia', 'minimo', 'precio', 'estado', 'acciones'];

  constructor() {
    this.api.proveedores().subscribe((p) => this.proveedores.set(p));
    this.api.productos({ soloActivos: false }).subscribe((ps) =>
      this.todasCategorias.set([...new Set(ps.map((p) => p.categoria).filter((c): c is string => !!c))].sort())
    );
    this.cargar();
  }

  cargar(): void {
    this.api
      .productos({ q: this.q, proveedorId: this.proveedorId, categoria: this.categoria, estado: this.estado, soloActivos: this.soloActivos })
      .subscribe((p) => this.productos.set(p));
  }

  ver(p: Producto): void {
    this.dialog.open(ProductoDetalleComponent, { data: p, width: '640px', maxWidth: '95vw' });
  }

  abrir(p: Producto | null): void {
    this.dialog
      .open(ProductoDialogComponent, { data: { producto: p, proveedores: this.proveedores() } })
      .afterClosed()
      .subscribe((r) => {
        if (r) {
          this.snack.open(p ? 'Producto actualizado' : 'Producto creado', 'OK', { duration: 3000 });
          this.cargar();
        }
      });
  }

  /** HU6: eliminación lógica; el producto conserva sus movimientos (RF-10). */
  deshabilitar(p: Producto): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          titulo: 'Deshabilitar producto',
          mensaje: `"${p.nombre}" dejará de admitir compras y ventas; conserva su historial y admite bajas.`,
          confirmar: 'Deshabilitar'
        }
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.api.deshabilitarProducto(p.id).subscribe(() => {
            this.snack.open('Producto deshabilitado', 'OK', { duration: 3000 });
            this.cargar();
          });
        }
      });
  }

  habilitar(p: Producto): void {
    this.api.habilitarProducto(p.id).subscribe({
      next: () => {
        this.snack.open('Producto habilitado', 'OK', { duration: 3000 });
        this.cargar();
      },
      error: (e) => {
        const err = errorDeApi(e);
        this.snack.open(`${err.message} ${err.accion}`, 'Cerrar', { duration: 8000 });
      }
    });
  }
}
