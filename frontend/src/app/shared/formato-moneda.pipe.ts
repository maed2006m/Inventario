import { Pipe, PipeTransform } from '@angular/core';

const FORMATO = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

/** Formatea pesos colombianos: 3900 → "$ 3.900". */
@Pipe({ name: 'moneda', standalone: true })
export class FormatoMonedaPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return valor === null || valor === undefined ? '—' : FORMATO.format(valor);
  }
}
