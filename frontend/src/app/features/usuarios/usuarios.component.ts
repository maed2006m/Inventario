import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/auth.service';
import { errorDeApi } from '../../core/errores';
import { Usuario } from '../../core/models';
import { UsuariosService } from '../../core/usuarios.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { UsuarioDialogComponent } from './usuario-dialog.component';

/**
 * Cuentas de la tienda (RF-30). Solo el Administrador entra aquí (rolGuard('ADMIN')).
 * Las cuentas no se borran: se desactivan, porque sus movimientos siguen en el historial.
 */
@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [
    DatePipe,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="pagina">
      <h1>Usuarios</h1>
      <div class="barra-acciones">
        <span class="espacio"></span>
        <button mat-flat-button color="primary" (click)="nueva()"><mat-icon>person_add</mat-icon> Nueva cuenta</button>
      </div>
      <div class="tabla-contenedor">
        <table mat-table [dataSource]="usuarios()">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let u">
              {{ u.nombre }}
              @if (esYo(u)) {
                <small>(tú)</small>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="username">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Usuario</th>
            <td mat-cell *matCellDef="let u" class="mono ocultar-movil">{{ u.username }}</td>
          </ng-container>
          <ng-container matColumnDef="rol">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Rol</th>
            <td mat-cell *matCellDef="let u" class="ocultar-movil">{{ u.rol === 'ADMIN' ? 'Administrador' : 'Tendero' }}</td>
          </ng-container>
          <ng-container matColumnDef="creado">
            <th mat-header-cell *matHeaderCellDef class="ocultar-movil">Creada</th>
            <td mat-cell *matCellDef="let u" class="ocultar-movil">{{ u.creadoEn | date: 'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let u">
              <span class="pill" [class.verde]="u.activo" [class.gris]="!u.activo">{{
                u.activo ? '● Activa' : '○ Inactiva'
              }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let u" class="num">
              @if (!esYo(u)) {
                @if (u.activo) {
                  <button mat-icon-button (click)="desactivar(u)" matTooltip="Desactivar" aria-label="Desactivar">
                    <mat-icon>person_off</mat-icon>
                  </button>
                } @else {
                  <button mat-icon-button (click)="activar(u)" matTooltip="Activar" aria-label="Activar">
                    <mat-icon>how_to_reg</mat-icon>
                  </button>
                }
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let row; columns: columnas"></tr>
        </table>
      </div>
    </div>
  `
})
export class UsuariosComponent {
  private auth = inject(AuthService);
  private api = inject(UsuariosService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  usuarios = signal<Usuario[]>([]);
  columnas = ['nombre', 'username', 'rol', 'creado', 'estado', 'acciones'];

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.api.listar().subscribe((u) => this.usuarios.set(u));
  }

  /** La propia cuenta no se puede desactivar: así la tienda nunca queda sin administrador. */
  esYo(u: Usuario): boolean {
    return u.username === this.auth.sesion()?.username;
  }

  nueva(): void {
    this.dialog
      .open(UsuarioDialogComponent)
      .afterClosed()
      .subscribe((u: Usuario | undefined) => {
        if (u) {
          this.snack.open(`Cuenta "${u.username}" creada`, 'OK', { duration: 3000 });
          this.cargar();
        }
      });
  }

  desactivar(u: Usuario): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          titulo: 'Desactivar cuenta',
          mensaje: `¿Desactivar la cuenta de "${u.nombre}"? Ya no podrá iniciar sesión; sus movimientos se conservan.`,
          confirmar: 'Desactivar'
        }
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.cambiar(this.api.desactivar(u.id), 'Cuenta desactivada');
        }
      });
  }

  activar(u: Usuario): void {
    this.cambiar(this.api.activar(u.id), 'Cuenta activada');
  }

  private cambiar(peticion: ReturnType<UsuariosService['activar']>, mensaje: string): void {
    peticion.subscribe({
      next: () => {
        this.snack.open(mensaje, 'OK', { duration: 3000 });
        this.cargar();
      },
      error: (e) => {
        const err = errorDeApi(e);
        this.snack.open(`${err.message} ${err.accion}`, 'Cerrar', { duration: 8000 });
      }
    });
  }
}
