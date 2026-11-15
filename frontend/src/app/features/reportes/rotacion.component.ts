import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ChartConfiguration } from 'chart.js';
import { errorDeApi } from '../../core/errores';
import { RotacionItem } from '../../core/models';
import { ReportesService } from '../../core/reportes.service';
import { fechaIso } from '../../shared/etiquetas';
import { FormatoMonedaPipe } from '../../shared/formato-moneda.pipe';
import { GraficoComponent } from '../../shared/grafico.component';

/** Periodos rápidos del selector; 'mes' va del día 1 del mes actual a hoy. */
type Periodo = 7 | 30 | 90 | 'mes';

/**
 * Ranking de rotación con selector de periodo (HU12, RF-25).
 *
 * Los totales del resumen son sumas de presentación sobre las filas que ya entrega
 * la API; el índice de rotación y el orden del ranking los calcula el backend.
 */
@Component({
  selector: 'app-rotacion',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, GraficoComponent, FormatoMonedaPipe],
  templateUrl: './rotacion.component.html',
  styleUrl: './rotacion.component.css'
})
export class RotacionComponent {
  private api = inject(ReportesService);

  readonly periodos: { valor: Periodo; texto: string }[] = [
    { valor: 7, texto: '7 días' },
    { valor: 30, texto: '30 días' },
    { valor: 90, texto: '90 días' },
    { valor: 'mes', texto: 'Este mes' }
  ];

  filas = signal<RotacionItem[]>([]);
  error = signal<{ message: string; accion: string } | null>(null);
  cargando = signal(true);
  /** Periodo rápido activo; null cuando el usuario escribió las fechas a mano. */
  activo = signal<Periodo | null>(30);
  hasta = fechaIso(new Date());
  desde = fechaIso(new Date(Date.now() - 29 * 86_400_000));

  /** Productos con al menos una unidad vendida, en el orden del backend. */
  vendidos = computed(() => this.filas().filter((r) => r.unidadesVendidas > 0));
  /** Productos sin salida en el periodo. */
  quietos = computed(() => this.filas().filter((r) => r.unidadesVendidas === 0));
  masVendido = computed(() => this.vendidos().at(0)?.productoNombre ?? '—');
  unidades = computed(() => this.vendidos().reduce((s, r) => s + r.unidadesVendidas, 0));
  ingresos = computed(() => this.vendidos().reduce((s, r) => s + r.ingresos, 0));
  private maximo = computed(() => Math.max(1, ...this.vendidos().map((r) => r.unidadesVendidas)));

  grafico = computed<ChartConfiguration | null>(() => {
    const top = this.vendidos().slice(0, 10);
    if (top.length === 0) {
      return null;
    }
    return {
      type: 'bar',
      data: {
        labels: top.map((r) => r.productoNombre),
        datasets: [
          { label: 'Unidades vendidas', data: top.map((r) => r.unidadesVendidas), backgroundColor: '#3b82f6', borderRadius: 6 }
        ]
      },
      options: { indexAxis: 'y', plugins: { legend: { display: false } } }
    } as ChartConfiguration;
  });

  constructor() {
    this.cargar();
  }

  /** Aplica un periodo rápido y consulta de inmediato. */
  elegir(p: Periodo): void {
    const hoy = new Date();
    const inicio = p === 'mes' ? new Date(hoy.getFullYear(), hoy.getMonth(), 1) : new Date(Date.now() - (p - 1) * 86_400_000);
    this.desde = fechaIso(inicio);
    this.hasta = fechaIso(hoy);
    this.activo.set(p);
    this.cargar();
  }

  /** Consulta con las fechas escritas a mano. */
  consultar(): void {
    this.activo.set(null);
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.cargando.set(true);
    this.api.rotacion(this.desde, this.hasta).subscribe({
      next: (r) => {
        this.filas.set(r);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(errorDeApi(e));
        this.cargando.set(false);
      }
    });
  }

  /** Ancho de la barra respecto al producto más vendido. */
  ancho(r: RotacionItem): number {
    return Math.round((100 * r.unidadesVendidas) / this.maximo());
  }

  /** Descarga el ranking que está en pantalla; no vuelve a consultar al servidor. */
  descargarCsv(): void {
    const celda = (v: string | number | null) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const lineas = [
      ['Posición', 'Producto', 'Proveedor', 'Unidades vendidas', 'Ingresos', 'Índice de rotación'],
      ...this.filas().map((r) => [r.posicion, r.productoNombre, r.proveedorNombre, r.unidadesVendidas, r.ingresos, r.indiceRotacion])
    ].map((fila) => fila.map(celda).join(';'));
    // El BOM hace que Excel lea bien las tildes.
    const blob = new Blob(['﻿' + lineas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `rotacion_${this.desde}_${this.hasta}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }
}
