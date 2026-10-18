import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { ConsolaApiService, RespuestaConsola } from './consola-api.service';

describe('ConsolaApiService', () => {
  let http: HttpTestingController;
  let api: ConsolaApiService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(ConsolaApiService);
  });

  afterEach(() => http.verify());

  it('envía el token elegido y omite los parámetros vacíos', () => {
    api.enviar({ metodo: 'GET', ruta: '/productos', query: { q: 'arroz', estado: '' }, token: 'xyz' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/productos');
    expect(req.request.headers.get('Authorization')).toBe('Bearer xyz');
    expect(req.request.params.get('q')).toBe('arroz');
    expect(req.request.params.has('estado')).toBeFalse();
    req.flush('[]');
  });

  it('devuelve los errores como respuesta con el cuerpo ApiError interpretado', () => {
    let r: RespuestaConsola | undefined;
    api.enviar({ metodo: 'POST', ruta: '/movimientos', cuerpo: { cantidad: 100 } }).subscribe((x) => (r = x));
    http
      .expectOne('/api/movimientos')
      .flush(JSON.stringify({ status: 409, message: 'Supera la existencia', accion: 'Reduzca la cantidad' }), {
        status: 409,
        statusText: 'Conflict'
      });
    expect(r?.status).toBe(409);
    expect(r?.ok).toBeFalse();
    expect((r?.cuerpo as { accion: string }).accion).toBe('Reduzca la cantidad');
  });

  it('un 401 no cierra la sesión porque no pasa por authInterceptor', () => {
    const auth = TestBed.inject(AuthService);
    const logout = spyOn(auth, 'logout');
    api.enviar({ metodo: 'GET', ruta: '/productos' }).subscribe();
    http.expectOne('/api/productos').flush('', { status: 401, statusText: 'Unauthorized' });
    expect(logout).not.toHaveBeenCalled();
  });

  it('deja como texto lo que no es JSON (CSV)', () => {
    expect(ConsolaApiService.interpretar('fecha,producto\n')).toBe('fecha,producto\n');
    expect(ConsolaApiService.interpretar('')).toBeNull();
  });
});
