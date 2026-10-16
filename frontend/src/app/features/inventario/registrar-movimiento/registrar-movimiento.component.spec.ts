import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Producto } from '../../../core/models';
import { RegistrarMovimientoComponent } from './registrar-movimiento.component';

describe('RegistrarMovimientoComponent (RN-02, RN-04, RNF-03)', () => {
  let fixture: ComponentFixture<RegistrarMovimientoComponent>;
  let c: RegistrarMovimientoComponent;
  let http: HttpTestingController;

  const leche: Producto = {
    id: 5, sku: 'LEC-001', nombre: 'Leche entera 1 L', categoria: 'Lácteos', proveedorId: 2,
    proveedorNombre: 'Lácteos del Valle', stockMinimo: 30, costoUnitario: 3100, precioVenta: 3900, activo: true,
    existencia: 26, estado: 'BAJO_STOCK'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegistrarMovimientoComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    });
    fixture = TestBed.createComponent(RegistrarMovimientoComponent);
    c = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    http.expectOne((r) => r.url === '/api/productos').flush([leche]);
    fixture.detectChanges();
  });

  function elegirLeche() {
    c.form.controls.productoId.setValue(5);
    http.expectOne('/api/productos/5').flush(leche);
  }

  it('marca superaExistencia cuando una salida supera la existencia', () => {
    elegirLeche();
    c.form.patchValue({ tipo: 'SALIDA', cantidad: 40 });
    expect(c.form.hasError('superaExistencia')).toBeTrue();
    expect(c.form.getError('superaExistencia')).toEqual({ disponible: 26 });
  });

  it('no marca error si la salida es igual a la existencia', () => {
    elegirLeche();
    c.form.patchValue({ tipo: 'SALIDA', cantidad: 26 });
    expect(c.form.hasError('superaExistencia')).toBeFalse();
  });

  it('no aplica la validación a las entradas', () => {
    elegirLeche();
    c.form.patchValue({ tipo: 'ENTRADA', cantidad: 400 });
    expect(c.form.hasError('superaExistencia')).toBeFalse();
  });

  it('cambia los motivos disponibles según el tipo (RN-04)', () => {
    c.form.controls.tipo.setValue('ENTRADA');
    expect(c.motivos()).toEqual(['COMPRA']);
    expect(c.form.controls.motivo.value).toBe('COMPRA');
    c.form.controls.tipo.setValue('SALIDA');
    expect(c.motivos()).toEqual(['VENTA', 'AVERIA', 'VENCIMIENTO']);
    expect(c.form.controls.motivo.value).toBe('VENTA');
  });

  it('rechaza cantidades menores que 1', () => {
    c.form.controls.cantidad.setValue(0);
    expect(c.form.controls.cantidad.invalid).toBeTrue();
  });

  it('muestra la causa y la acción cuando el servidor responde 409', () => {
    elegirLeche();
    c.form.patchValue({ tipo: 'SALIDA', motivo: 'VENTA', cantidad: 5 });
    const form = fixture.debugElement.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    http.expectOne('/api/movimientos').flush(
      { status: 409, message: 'La salida de 5 unidades supera la existencia disponible (2).', accion: 'Reduzca la cantidad a 2 o menos.' },
      { status: 409, statusText: 'Conflict' }
    );
    fixture.detectChanges();
    expect(c.errorServidor()?.accion).toContain('Reduzca');
    expect(fixture.nativeElement.textContent).toContain('supera la existencia');
  });
});
