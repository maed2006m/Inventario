import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorDeApi } from '../../core/errores';
import { environment } from '../../../environments/environment';

/** Tiempo que la portada ocupa toda la pantalla antes de dividirse (ms). */
const ESPERA_PORTADA_MS = 500;

/**
 * Inicio de sesión (Ilustración 2, RNF-09).
 *
 * La vista abre con una portada oscura a pantalla completa y, tras una breve
 * espera, se divide: la marca queda a la izquierda y el formulario entra por la
 * derecha. La animación es solo CSS; aquí únicamente se cambia la señal `dividido`.
 * La lógica de autenticación no cambia: sigue delegando en AuthService.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private temporizador?: ReturnType<typeof setTimeout>;

  /** false = portada a pantalla completa; true = pantalla dividida con el formulario. */
  dividido = signal(false);
  /** Muestra u oculta el texto de la contraseña. */
  ver = signal(false);
  cargando = signal(false);
  /** Solo en desarrollo: en producción las contraseñas de demostración ya no sirven. */
  readonly usuariosDemo = environment.usuariosDemo;
  error = signal<{ message: string; accion: string } | null>(null);

  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  ngOnInit(): void {
    this.temporizador = setTimeout(() => this.dividido.set(true), ESPERA_PORTADA_MS);
  }

  ngOnDestroy(): void {
    clearTimeout(this.temporizador);
  }

  /** Indica si un campo debe mostrarse en error: inválido y ya tocado por el usuario. */
  invalido(campo: 'username' | 'password'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && control.touched;
  }

  entrar(): void {
    if (this.form.invalid) {
      // Marca los campos para que aparezcan los mensajes de validación.
      this.form.markAllAsTouched();
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
