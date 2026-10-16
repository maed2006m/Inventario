import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorDeApi } from '../../core/errores';

/** Inicio de sesión (Ilustración 2, RNF-09). */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="fondo">
      <form class="tarjeta tactil" [formGroup]="form" (ngSubmit)="entrar()">
        <div class="marca"><mat-icon>storefront</mat-icon> Inventario Tienda de Barrio</div>
        <h1>Iniciar sesión</h1>
        <mat-form-field appearance="outline">
          <mat-label>Usuario</mat-label>
          <input matInput formControlName="username" autocomplete="username" />
          @if (form.controls.username.hasError('required')) {
            <mat-error>El usuario es obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Contraseña</mat-label>
          <input matInput [type]="ver() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" />
          <button mat-icon-button matSuffix type="button" (click)="ver.set(!ver())" [attr.aria-label]="ver() ? 'Ocultar' : 'Mostrar'">
            <mat-icon>{{ ver() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.controls.password.hasError('required')) {
            <mat-error>La contraseña es obligatoria</mat-error>
          }
        </mat-form-field>
        @if (error()) {
          <div class="error-servidor" role="alert">
            <strong>{{ error()!.message }}</strong>{{ error()!.accion }}
          </div>
        }
        @if (cargando()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || cargando()">Entrar</button>
        <p class="ayuda">Usuarios de demostración: admin / Admin123* · tendero / Tendero123*</p>
      </form>
    </div>
  `,
  styles: [
    `
      .fondo { min-height: 100vh; display: grid; place-items: center; padding: 16px; box-sizing: border-box; }
      .tarjeta { width: 100%; max-width: 380px; background: #fff; border: 1px solid var(--borde); border-radius: 12px;
        padding: 28px 24px; display: flex; flex-direction: column; gap: 6px; }
      .marca { display: flex; align-items: center; gap: 8px; color: var(--verde); font-weight: 500; }
      h1 { font-size: 1.4rem; font-weight: 500; margin: 8px 0 16px; }
    `
  ]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  ver = signal(false);
  cargando = signal(false);
  error = signal<{ message: string; accion: string } | null>(null);

  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  entrar(): void {
    if (this.form.invalid) {
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(errorDeApi(e));
      }
    });
  }
}
