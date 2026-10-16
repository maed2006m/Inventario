import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse } from './models';

describe('AuthService (RNF-09)', () => {
  let http: HttpTestingController;
  const respuesta: LoginResponse = { token: 'abc', username: 'admin', nombre: 'Administrador', rol: 'ADMIN', expiraEnMinutos: 120 };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('guarda la sesión en sessionStorage al iniciar sesión', () => {
    const auth = TestBed.inject(AuthService);
    auth.login('admin', 'Admin123*').subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'Admin123*' });
    req.flush(respuesta);

    expect(auth.sesion()?.username).toBe('admin');
    expect(auth.token()).toBe('abc');
    expect(auth.esAdmin()).toBeTrue();
    expect(JSON.parse(sessionStorage.getItem('sesion')!).token).toBe('abc');
  });

  it('borra la sesión y vuelve al login al cerrar sesión', () => {
    sessionStorage.setItem('sesion', JSON.stringify(respuesta));
    const auth = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    expect(auth.sesion()).not.toBeNull();
    auth.logout();

    expect(auth.sesion()).toBeNull();
    expect(sessionStorage.getItem('sesion')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('tolera un valor corrupto en sessionStorage', () => {
    sessionStorage.setItem('sesion', '{no es json');
    expect(TestBed.inject(AuthService).sesion()).toBeNull();
  });
});
