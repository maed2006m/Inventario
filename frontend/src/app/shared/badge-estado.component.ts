import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { EstadoStock } from '../core/models';

const TEXTO: Record<EstadoStock, string> = { EN_STOCK: 'En stock', BAJO_STOCK: 'Bajo stock', AGOTADO: 'Agotado' };
const CLASE: Record<EstadoStock, string> = { EN_STOCK: 'ok', BAJO_STOCK: 'warn', AGOTADO: 'bad' };

/**
 * Insignia del estado de stock (RF-23, Ilustración 4). Nunca comunica el estado solo con
 * color: siempre lleva texto e ícono, por accesibilidad.
 */
@Component({
  selector: 'app-badge-estado',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'badge ' + clase()" [attr.aria-label]="'Estado: ' + texto()"
    ><span class="punto" aria-hidden="true">{{ icono() }}</span>{{ texto() }}</span
  >`,
  styles: [
    `
      .badge { display: inline-flex; align-items: center; gap: 4px; padding: 2px 10px; border-radius: 999px;
        font-size: 0.8rem; font-weight: 500; white-space: nowrap; }
      .punto { font-size: 0.75rem; }
      .ok { background: var(--verde-claro); color: var(--verde); }
      .warn { background: var(--ambar-claro); color: var(--ambar); }
      .bad { background: var(--rojo-claro); color: var(--rojo); }
    `
  ]
})
export class BadgeEstadoComponent {
  estado = input.required<EstadoStock>();
  texto = computed(() => TEXTO[this.estado()]);
  clase = computed(() => CLASE[this.estado()]);
  icono = computed(() => ({ EN_STOCK: '●', BAJO_STOCK: '▲', AGOTADO: '✕' })[this.estado()]);
}
