import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, viewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

/** Paleta categórica compartida por los gráficos. */
export const PALETA = ['#1f6f5c', '#3b82c4', '#d08a2e', '#8e5bb5', '#c4504f', '#5c8a3a', '#6b7280'];

/**
 * Envoltorio mínimo de Chart.js como componente standalone. Recibe la configuración
 * completa y redibuja cuando cambia. Se usa en el tablero y en los reportes.
 */
@Component({
  selector: 'app-grafico',
  standalone: true,
  template: `<div class="lienzo" [style.height.px]="alto()"><canvas #lienzo [attr.aria-label]="titulo()" role="img"></canvas></div>`,
  styles: [`.lienzo { position: relative; width: 100%; }`]
})
export class GraficoComponent implements AfterViewInit, OnDestroy {
  config = input.required<ChartConfiguration>();
  alto = input(280);
  titulo = input('Gráfico');

  private lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private chart: Chart | null = null;
  private listo = false;

  constructor() {
    effect(() => {
      const c = this.config();
      if (this.listo) {
        this.dibujar(c);
      }
    });
  }

  ngAfterViewInit(): void {
    this.listo = true;
    this.dibujar(this.config());
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private dibujar(c: ChartConfiguration): void {
    this.chart?.destroy();
    this.chart = new Chart(this.lienzo().nativeElement, {
      ...c,
      options: { responsive: true, maintainAspectRatio: false, ...(c.options ?? {}) }
    } as ChartConfiguration);
  }
}
