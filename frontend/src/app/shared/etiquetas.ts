import { MotivoMovimiento, TipoMovimiento } from '../core/models';

/** Textos legibles para los enumerados del backend. */
export const ETIQUETA_MOTIVO: Record<MotivoMovimiento, string> = {
  COMPRA: 'Compra',
  VENTA: 'Venta',
  AVERIA: 'Avería',
  VENCIMIENTO: 'Vencimiento'
};

export const ETIQUETA_TIPO: Record<TipoMovimiento, string> = { ENTRADA: 'Entrada', SALIDA: 'Salida' };

/** Motivos válidos según el tipo (RN-04). */
export function motivosDe(tipo: TipoMovimiento): MotivoMovimiento[] {
  return tipo === 'ENTRADA' ? ['COMPRA'] : ['VENTA', 'AVERIA', 'VENCIMIENTO'];
}

/** Fecha local AAAA-MM-DD (para los reportes). */
export function fechaIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** Color de la píldora (.pill de styles.scss) para cada motivo y tipo de movimiento. */
export const CLASE_MOTIVO: Record<MotivoMovimiento, string> = {
  COMPRA: 'verde',
  VENTA: 'morado',
  AVERIA: 'ambar',
  VENCIMIENTO: 'rojo'
};

export const CLASE_TIPO: Record<TipoMovimiento, string> = { ENTRADA: 'verde', SALIDA: 'morado' };
