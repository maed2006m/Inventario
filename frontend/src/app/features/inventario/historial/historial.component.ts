import { DatePipe } from '@angular/common';
import { HttpEventType } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { AuthService } from '../../../core/auth.service';
import { InventarioService } from '../../../core/inventario.service';
import {
  FiltrosMovimiento,
  MotivoMovimiento,
  Movimiento,
  Pagina,
  Producto,
  Proveedor,
  TipoMovimiento
} from '../../../core/models';
import { ETIQUETA_MOTIVO, ETIQUETA_TIPO } from '../../../shared/etiquetas';
import { FormatoMonedaPipe } from '../../../shared/formato-moneda.pipe';

/**
 * Historial de movimientos (Ilustración 7, HU14, RF-27, RF-28) con exportación CSV que
 * no bloquea la interfaz (HU15, RNF-07). Es de solo lectura: sin editar ni eliminar (RNF-04).
 */
@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [
    FormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule,
    FormatoMonedaPipe
  ],
  template: `
    <div class="pagina">
      <h1>Historial de movimientos</h1>
      <div class="barra-acciones">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Producto</mat-label>
          <mat-select [(ngModel)]="filtros.productoId" (selectionChange)="buscar()">
            <mat-option [value]="null">Todos</mat-option>
            @for (p of productos(); track p.id) {
              <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Proveedor</mat-label>
          <mat-select [(ngModel)]="filtros.proveedorId" (selectionChange)="buscar()">
            <mat-option [value]="null">Todos</mat-option>
            @for (p of proveedores(); track p.id) {
              <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Tipo</mat-label>
          <mat-select [(ngModel)]="filtros.tipo" (selectionChange)="buscar()">
            <mat-option [value]="null">Todos</mat-option>
            <mat-option value="ENTRADA">Entrada</mat-option>
            <mat-option value="SALIDA">Salida</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Motivo</mat-label>
          <mat-select [(ngModel)]="filtros.motivo" (selectionChange)="buscar()">
            <mat-option [value]="null">Todos</mat-option>
            @for (m of motivos; track m) {
              <mat-option [value]="m">{{ etiquetaMotivo[m] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [(ngModel)]="desde" (change)="buscar()" />
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [(ngModel)]="hasta" (change)="buscar()" />
        </mat-form-field>
        <span class="espacio"></span>
        @if (auth.esAdmin()) {
          <button mat-stroked-button (click)="exportarCsv()" [disabled]="exportando()">
            <mat-icon>download</mat-icon> Exportar CSV
          </button>
        }
      </div>
      @if (exportando()) {
        <div class="progreso">
          <mat-progress-bar [mode]="progreso() === null ? 'indeterminate' : 'determinate'" [value]="progreso() ?? 0" />
          <span class="ayuda">Generando CSV… puede seguir usando la aplicación.</span>
        </div>
      }
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="pagina()?.content ?? []">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let m">{{ m.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="producto">
            <th mat-header-cell *matHeaderCellDef>Producto</th>
            <td mat-cell *matCellDef="let m">{{ m.productoNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let m" [class.salida]="m.tipo === 'SALIDA'">{{ etiquetaTipo[m.tipo] }}</td>
          </ng-container>
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Motivo</th>
            <td mat-cell *matCellDef="let m" class="ocultar-movil">{{ etiquetaMotivo[m.motivo] }}</td>
          </ng-container>
          <ng-container matColumnDef="cantidad">
            <th mat-header-cell *matHeaderCellDef class="num">Cantidad</th>
            <td mat-cell *matCellDef="let m" class="num">{{ m.cantidad }}</td>
          </ng-container>
          <ng-container matColumnDef="valor">
            <th mat-header-cell *matHeaderCellDef class="num ocultar-movil">Valor unitario</th>
            <td mat-cell *matCellDef="let m" class="num ocultar-movil">{{ m.valorUnitario | moneda }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Usuario</th>
            <td mat-cell *matCellDef="let m" class="ocultar-movil">{{ m.usuario }}</td>
          </ng-container>
          <ng-container matColumnDef="observacion">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Observación</th>
            <td mat-cell *matCellDef="let m" class="ocultar-movil">{{ m.observacion ?? '' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let row; columns: columnas"></tr>
        </table>
        @if (pagina()?.totalElements === 0) {
          <p class="vacio">No hay movimientos con estos filtros.</p>
        }
      </div>
      <mat-paginator
        [length]="pagina()?.totalElements ?? 0"
        [pageIndex]="page"
        [pageSize]="size"
        [pageSizeOptions]="[10, 20, 50, 100]"
        (page)="cambiarPagina($event)"
      />
    </div>
  `,
  styles: [
    `
      .salida { color: var(--rojo); }
      .progreso { display: grid; gap: 4px; margin-bottom: 12px; }
    `
  ]
})
export class HistorialComponent {
  auth = inject(AuthService);
  private api = inject(InventarioService);
  private snack = inject(MatSnackBar);

  productos = signal<Producto[]>([]);
  proveedores = signal<Proveedor[]>([]);
  pagina = signal<Pagina<Movimiento> | null>(null);
  exportando = signal(false);
  progreso = signal<number | null>(null);

  filtros: { productoId: number | null; proveedorId: number | null; tipo: TipoMovimiento | null; motivo: MotivoMovimiento | null } =
    { productoId: null, proveedorId: null, tipo: null, motivo: null };
  desde = '';
  hasta = '';
  page = 0;
  size = 20;
  columnas = ['fecha', 'producto', 'tipo', 'motivo', 'cantidad', 'valor', 'usuario', 'observacion'];
  motivos: MotivoMovimiento[] = ['COMPRA', 'VENTA', 'AVERIA', 'VENCIMIENTO'];
  etiquetaMotivo: Record<string, string> = ETIQUETA_MOTIVO;
  etiquetaTipo: Record<string, string> = ETIQUETA_TIPO;

  constructor() {
    this.api.productos({ soloActivos: false }).subscribe((p) => this.productos.set(p));
    this.api.proveedores().subscribe((p) => this.proveedores.set(p));
    this.cargar();
  }

  /** Filtros con fechas en ISO 8601 (UTC); "hasta" incluye todo el día elegido. */
  filtrosActuales(): FiltrosMovimiento {
    return {
      ...this.filtros,
      desde: this.desde ? new Date(`${this.desde}T00:00:00`).toISOString() : null,
      hasta: this.hasta ? new Date(`${this.hasta}T23:59:59.999`).toISOString() : null
    };
  }

  buscar(): void {
    this.page = 0;
    this.cargar();
  }

  cambiarPagina(e: PageEvent): void {
    this.page = e.pageIndex;
    this.size = e.pageSize;
    this.cargar();
  }

  cargar(): void {
    this.api.historial(this.filtrosActuales(), this.page, this.size).subscribe((p) => this.pagina.set(p));
  }

  /** Descarga con eventos de progreso; la interfaz sigue disponible mientras tanto (RNF-07). */
  exportarCsv(): void {
    this.exportando.set(true);
    this.progreso.set(null);
    this.api.exportarCsv(this.filtrosActuales()).subscribe({
      next: (ev) => {
        if (ev.type === HttpEventType.DownloadProgress && ev.total) {
          this.progreso.set(Math.round((100 * ev.loaded) / ev.total));
        }
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
}
