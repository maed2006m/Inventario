import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { errorDeApi } from '../../core/errores';
import { Rol } from '../../core/models';
import { UsuariosService } from '../../core/usuarios.service';

/** Formulario modal para crear una cuenta de tendero o administrador (RF-30). */
@Component({
  selector: 'app-usuario-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Nueva cuenta</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos tactil">
        <mat-form-field appearance="outline">
          <mat-label>Nombre completo</mat-label>
          <input matInput formControlName="nombre" maxlength="120" />
          <mat-error>El nombre es obligatorio</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Usuario</mat-label>
          <input matInput formControlName="username" autocapitalize="none" autocomplete="off" spellcheck="false" />
          <mat-hint>Con este nombre inicia sesión. Sin espacios.</mat-hint>
          <mat-error>Entre 3 y 50 caracteres: letras, números, punto o guion, sin espacios</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Contraseña</mat-label>
          <input
            matInput
            formControlName="password"
            [type]="verClave() ? 'text' : 'password'"
            autocomplete="new-password"
          />
          <button
            mat-icon-button
            matSuffix
            type="button"
            (click)="verClave.set(!verClave())"
            [attr.aria-label]="verClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
          >
            <mat-icon>{{ verClave() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-hint>Mínimo 8 caracteres</mat-hint>
          <mat-error>Entre 8 y 72 caracteres</mat-error>
        </mat-form-field>
        <div class="rol">
          <span class="etiqueta">Rol</span>
          <mat-button-toggle-group formControlName="rol" aria-label="Rol">
            <mat-button-toggle value="TENDERO">Tendero</mat-button-toggle>
            <mat-button-toggle value="ADMIN">Administrador</mat-button-toggle>
          </mat-button-toggle-group>
          <small>
            @if (form.value.rol === 'ADMIN') {
              Ve y maneja todo, incluidos reportes y cuentas, como el dueño.
            } @else {
              Registra movimientos y consulta el inventario. No ve reportes ni cuentas.
            }
          </small>
        </div>
        @if (error(); as e) {
          <div class="error-servidor" role="alert">
            <strong>{{ e.message }}</strong
            >{{ e.accion }}
          </div>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || guardando()">
          Crear cuenta
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [
    `
      .campos {
        display: grid;
        gap: 4px;
        min-width: min(420px, 80vw);
      }
      .rol {
        display: grid;
        gap: 6px;
        margin: 8px 0 12px;
      }
      .rol .etiqueta {
        font-weight: 500;
      }
      .rol small {
        color: var(--mat-sys-on-surface-variant, #555);
      }
    `
  ]
})
export class UsuarioDialogComponent {
  private ref = inject<MatDialogRef<UsuarioDialogComponent>>(MatDialogRef);
  private api = inject(UsuariosService);
  private fb = inject(FormBuilder);

  guardando = signal(false);
  verClave = signal(false);
  error = signal<{ message: string; accion: string } | null>(null);

  form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._-]{3,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    rol: ['TENDERO' as Rol, Validators.required]
  });

  guardar(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.api.crear({ ...v, nombre: v.nombre.trim(), username: v.username.trim() }).subscribe({
      next: (u) => this.ref.close(u),
      error: (e) => {
        this.guardando.set(false);
        this.error.set(errorDeApi(e));
      }
    });
  }
}
