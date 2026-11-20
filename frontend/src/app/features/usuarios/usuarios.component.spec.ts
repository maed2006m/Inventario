import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LoginResponse, Usuario } from '../../core/models';
import { UsuarioDialogComponent } from './usuario-dialog.component';
import { UsuariosComponent } from './usuarios.component';

const sesionAdmin: LoginResponse = {
  token: 't',
  username: 'admin',
  nombre: 'Administrador',
  rol: 'ADMIN',
  expiraEnMinutos: 120
};

const cuentas: Usuario[] = [
  { id: 1, username: 'admin', nombre: 'Administrador', rol: 'ADMIN', activo: true, creadoEn: '2026-11-01T10:00:00Z' },
  { id: 2, username: 'tendero', nombre: 'Tendero', rol: 'TENDERO', activo: true, creadoEn: '2026-11-01T10:00:00Z' },
  { id: 3, username: 'miguel', nombre: 'Miguel', rol: 'TENDERO', activo: false, creadoEn: '2026-11-19T10:00:00Z' }
];

function configurar(extra: unknown[] = []) {
  sessionStorage.setItem('sesion', JSON.stringify(sesionAdmin));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations(), ...extra]
  });
  return TestBed.inject(HttpTestingController);
}

describe('UsuariosComponent (RF-30)', () => {
  afterEach(() => sessionStorage.clear());

  it('lista las cuentas y no ofrece desactivar la propia', () => {
    const http = configurar();
    const fixture = TestBed.createComponent(UsuariosComponent);
    http.expectOne('/api/usuarios').flush(cuentas);
    fixture.detectChanges();

    const filas = fixture.nativeElement.querySelectorAll('tr.mat-mdc-row');
    expect(filas.length).toBe(3);
    expect(filas[0].textContent).toContain('(tú)');
    expect(filas[0].querySelector('button')).toBeNull();
    expect(filas[1].querySelector('[aria-label="Desactivar"]')).not.toBeNull();
    expect(filas[2].querySelector('[aria-label="Activar"]')).not.toBeNull();
  });

  it('activar llama al backend y recarga el listado', () => {
    const http = configurar();
    const fixture = TestBed.createComponent(UsuariosComponent);
    http.expectOne('/api/usuarios').flush(cuentas);

    fixture.componentInstance.activar(cuentas[2]);
    const req = http.expectOne('/api/usuarios/3/activar');
    expect(req.request.method).toBe('PUT');
    req.flush({ ...cuentas[2], activo: true });
    http.expectOne('/api/usuarios').flush(cuentas);
    http.verify();
  });
});

describe('UsuarioDialogComponent (RF-30)', () => {
  let cerrar: jasmine.Spy;

  beforeEach(() => {
    cerrar = jasmine.createSpy('close');
  });
  afterEach(() => sessionStorage.clear());

  function crear() {
    const http = configurar([{ provide: MatDialogRef, useValue: { close: cerrar } }]);
    const fixture = TestBed.createComponent(UsuarioDialogComponent);
    fixture.detectChanges();
    return { http, c: fixture.componentInstance, fixture };
  }

  it('rechaza usuario con espacios y contraseña corta, y por defecto el rol es tendero', () => {
    const { c } = crear();
    expect(c.form.value.rol).toBe('TENDERO');
    c.form.setValue({ nombre: 'Miguel', username: 'mi guel', password: 'corta', rol: 'TENDERO' });
    expect(c.form.controls.username.invalid).toBeTrue();
    expect(c.form.controls.password.invalid).toBeTrue();
  });

  it('envía la cuenta y cierra el diálogo con la cuenta creada', () => {
    const { c, http } = crear();
    c.form.setValue({ nombre: ' Miguel Sanabria ', username: 'miguel', password: 'Clave123*', rol: 'ADMIN' });
    c.guardar();

    const req = http.expectOne('/api/usuarios');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombre: 'Miguel Sanabria',
      username: 'miguel',
      password: 'Clave123*',
      rol: 'ADMIN'
    });
    req.flush(cuentas[2]);
    expect(cerrar).toHaveBeenCalledWith(cuentas[2]);
  });

  it('muestra el mensaje y la acción del backend si el usuario ya existe (RNF-03)', () => {
    const { c, http, fixture } = crear();
    c.form.setValue({ nombre: 'Otro', username: 'tendero', password: 'Clave123*', rol: 'TENDERO' });
    c.guardar();

    http
      .expectOne('/api/usuarios')
      .flush(
        {
          status: 409,
          error: 'Conflict',
          message: "Ya existe una cuenta con el usuario 'tendero'.",
          accion: 'Elija otro nombre de usuario.'
        },
        { status: 409, statusText: 'Conflict' }
      );
    fixture.detectChanges();

    expect(cerrar).not.toHaveBeenCalled();
    expect(c.guardando()).toBeFalse();
    expect(fixture.nativeElement.querySelector('.error-servidor').textContent).toContain(
      'Elija otro nombre de usuario.'
    );
  });
});
