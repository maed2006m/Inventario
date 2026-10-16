import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EstadoStock } from '../core/models';
import { BadgeEstadoComponent } from './badge-estado.component';

describe('BadgeEstadoComponent (RF-23)', () => {
  let fixture: ComponentFixture<BadgeEstadoComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BadgeEstadoComponent] });
    fixture = TestBed.createComponent(BadgeEstadoComponent);
  });

  const casos: [EstadoStock, string, string][] = [
    ['EN_STOCK', 'En stock', 'ok'],
    ['BAJO_STOCK', 'Bajo stock', 'warn'],
    ['AGOTADO', 'Agotado', 'bad']
  ];

  casos.forEach(([estado, texto, clase]) => {
    it(`muestra "${texto}" con texto y no solo color para ${estado}`, () => {
      fixture.componentRef.setInput('estado', estado);
      fixture.detectChanges();
      const span: HTMLElement = fixture.nativeElement.querySelector('span');
      expect(span.textContent).toContain(texto);
      expect(span.classList).toContain(clase);
      expect(span.getAttribute('aria-label')).toBe(`Estado: ${texto}`);
    });
  });
});
