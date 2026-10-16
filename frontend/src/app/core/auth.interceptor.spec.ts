import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('sesion', JSON.stringify({ token: 'xyz', username: 'tendero', nombre: 'T', rol: 'TENDERO', expiraEnMinutos: 1 }));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => sessionStorage.clear());

  it('adjunta el token Bearer', () => {
    http.get('/api/productos').subscribe();
    const req = ctrl.expectOne('/api/productos');
    expect(req.request.headers.get('Authorization')).toBe('Bearer xyz');
    req.flush([]);
  });

  it('cierra la sesión ante un 401', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'logout');
    http.get('/api/productos').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/productos').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
  });
});
