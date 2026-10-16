import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API } from './api.config';
import { LoginResponse } from './models';

const CLAVE = 'sesion';

/**
 * Sesión del usuario. El token se guarda en sessionStorage (se borra al cerrar la
 * pestaña), nunca en localStorage, como pide la sección 8 de la guía.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private _sesion = signal<LoginResponse | null>(AuthService.leer());

  readonly sesion = this._sesion.asReadonly();
  readonly esAdmin = computed(() => this._sesion()?.rol === 'ADMIN');
  readonly token = computed(() => this._sesion()?.token ?? null);

  /** Inicia sesión y guarda la respuesta (token, nombre y rol). */
  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API}/auth/login`, { username, password }).pipe(
      tap((r) => {
        sessionStorage.setItem(CLAVE, JSON.stringify(r));
        this._sesion.set(r);
      })
    );
  }

  /** Cierra la sesión y vuelve al login (RF-31 propuesto). */
  logout(): void {
    sessionStorage.removeItem(CLAVE);
    this._sesion.set(null);
    this.router.navigate(['/login']);
  }

  private static leer(): LoginResponse | null {
    try {
      return JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null');
    } catch {
      return null;
    }
  }
}
