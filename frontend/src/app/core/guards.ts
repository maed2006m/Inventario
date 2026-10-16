import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './models';
import { AuthService } from './auth.service';

/** Solo deja pasar con sesión iniciada; si no, redirige a /login (RNF-09). */
export const authGuard: CanActivateFn = () =>
  inject(AuthService).sesion() ? true : inject(Router).parseUrl('/login');

/** Solo deja pasar al rol indicado; si no, redirige al tablero (RNF-10). */
export const rolGuard =
  (rol: Rol): CanActivateFn =>
  () =>
    inject(AuthService).sesion()?.rol === rol ? true : inject(Router).parseUrl('/dashboard');
