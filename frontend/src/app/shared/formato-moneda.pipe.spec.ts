import { FormatoMonedaPipe } from './formato-moneda.pipe';
import { fechaIso, motivosDe } from './etiquetas';

describe('FormatoMonedaPipe y utilidades', () => {
  const pipe = new FormatoMonedaPipe();

  it('formatea pesos colombianos sin decimales', () => {
    const texto = pipe.transform(3900);
    expect(texto).toContain('3.900');
    expect(texto).toContain('$');
  });

  it('muestra un guion si no hay valor', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
  });

  it('RN-04: los motivos dependen del tipo', () => {
    expect(motivosDe('ENTRADA')).toEqual(['COMPRA']);
    expect(motivosDe('SALIDA')).toEqual(['VENTA', 'AVERIA', 'VENCIMIENTO']);
  });

  it('fechaIso usa AAAA-MM-DD local', () => {
    expect(fechaIso(new Date(2026, 9, 5))).toBe('2026-10-05');
  });
});
