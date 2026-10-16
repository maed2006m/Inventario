import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, rolGuard } from './guards';
import { LoginResponse } from './models';

describe('Guards (RNF-09, RNF-10)', () => {
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = {} as RouterStateSnapshot;
  const sesion = (rol: 'ADMIN' | 'TENDERO'): LoginResponse => ({
    token: 't',
    username: rol.toLowerCase(),
    nombre: rol,
    rol,
    expiraEnMinutos: 120
  });

  function preparar(s: LoginResponse | null) {
    sessionStorage.clear();
    if (s) {
      sessionStorage.setItem('sesion', JSON.stringify(s));
    }
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    TestBed.inject(AuthService);
    TestBed.inject(HttpTestingController);
  }

  const ejecutar = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(ruta, estado));
  const url = (r: unknown) => TestBed.inject(Router).serializeUrl(r as UrlTree);

  it('authGuard redirige a /login sin sesión', () => {
    preparar(null);
    expect(url(ejecutar(authGuard))).toBe('/login');
  });

  it('authGuard deja pasar con sesión', () => {
    preparar(sesion('TENDERO'));
    expect(ejecutar(authGuard)).toBeTrue();
  });

  it("rolGuard('ADMIN') bloquea al tendero y lo envía al tablero", () => {
    preparar(sesion('TENDERO'));
    expect(url(ejecutar(rolGuard('ADMIN')))).toBe('/dashboard');
  });

  it("rolGuard('ADMIN') deja pasar al administrador", () => {
    preparar(sesion('ADMIN'));
    expect(ejecutar(rolGuard('ADMIN'))).toBeTrue();
  });
});
