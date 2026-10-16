import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/auth.service';
import { errorDeApi } from '../../../core/errores';
import { InventarioService } from '../../../core/inventario.service';
import { Proveedor } from '../../../core/models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { ProveedorDialogComponent } from './proveedor-dialog.component';

/**
 * Proveedores (Épica 1). Ambos roles crean (HU1); solo el Administrador edita (HU2) e
 * inactiva (HU3), igual que la matriz de permisos del backend.
 */
@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="pagina">
      <h1>Proveedores</h1>
      <div class="barra-acciones">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Buscar por nombre o NIT</mat-label>
          <input matInput [(ngModel)]="q" (ngModelChange)="filtrar()" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-slide-toggle [(ngModel)]="soloActivos" (change)="cargar()">Solo activos</mat-slide-toggle>
        <span class="espacio"></span>
        <button mat-flat-button color="primary" (click)="abrir(null)"><mat-icon>add</mat-icon> Nuevo proveedor</button>
      </div>
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="proveedores()">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let p">{{ p.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="nit">
            <th mat-header-cell *matHeaderCellDef>NIT</th>
            <td mat-cell *matCellDef="let p">{{ p.nit }}</td>
          </ng-container>
          <ng-container matColumnDef="telefono">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Teléfono</th>
            <td mat-cell *matCellDef="let p" class="ocultar-movil">{{ p.telefono ?? '—' }}</td>
          </ng-container>
          <ng-container matColumnDef="entrega">
            <th mat-header-cell *matHeaderCellDef class="num ocultar-movil">Entrega (días)</th>
            <td mat-cell *matCellDef="let p" class="num ocultar-movil">{{ p.tiempoEntregaDias }}</td>
          </ng-container>
          <ng-container matColumnDef="productos">
            <th mat-header-cell *matHeaderCellDef class="num">Productos activos</th>
            <td mat-cell *matCellDef="let p" class="num">{{ p.productosActivos }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let p">{{ p.activo ? 'Activo' : 'Inactivo' }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let p" class="num">
              @if (auth.esAdmin()) {
                <button mat-icon-button (click)="abrir(p)" matTooltip="Editar" aria-label="Editar"><mat-icon>edit</mat-icon></button>
                @if (p.activo) {
                  <button mat-icon-button (click)="inactivar(p)" matTooltip="Inactivar" aria-label="Inactivar">
                    <mat-icon>block</mat-icon>
                  </button>
                }
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let row; columns: columnas"></tr>
        </table>
        @if (proveedores().length === 0) {
          <p class="vacio">No hay proveedores que coincidan.</p>
        }
      </div>
    </div>
  `
})
export class ProveedoresComponent {
  auth = inject(AuthService);
  private api = inject(InventarioService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  proveedores = signal<Proveedor[]>([]);
  private todos: Proveedor[] = [];
  q = '';
  soloActivos = false;
  columnas = ['nombre', 'nit', 'telefono', 'entrega', 'productos', 'estado', 'acciones'];

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.api.proveedores(this.soloActivos).subscribe((p) => {
      this.todos = p;
      this.filtrar();
    });
  }

  /** Búsqueda por nombre o NIT en el navegador (el backend lista sin filtro de texto). */
  filtrar(): void {
    const q = this.q.trim().toLowerCase();
    this.proveedores.set(q ? this.todos.filter((p) => p.nombre.toLowerCase().includes(q) || p.nit.includes(q)) : this.todos);
  }

  abrir(p: Proveedor | null): void {
    this.dialog
      .open(ProveedorDialogComponent, { data: p })
      .afterClosed()
      .subscribe((r) => {
        if (r) {
          this.snack.open(p ? 'Proveedor actualizado' : 'Proveedor creado', 'OK', { duration: 3000 });
          this.cargar();
        }
      });
  }

  /** HU3: eliminación lógica; el backend responde 409 si aún tiene productos activos (RN-07). */
  inactivar(p: Proveedor): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: { titulo: 'Inactivar proveedor', mensaje: `¿Inactivar a "${p.nombre}"? Su historial se conserva.`, confirmar: 'Inactivar' }
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.inactivarProveedor(p.id).subscribe({
          next: () => {
            this.snack.open('Proveedor inactivado', 'OK', { duration: 3000 });
            this.cargar();
          },
          error: (e) => {
            const err = errorDeApi(e);
            this.snack.open(`${err.message} ${err.accion}`, 'Cerrar', { duration: 8000 });
          }
        });
      });
  }
}
