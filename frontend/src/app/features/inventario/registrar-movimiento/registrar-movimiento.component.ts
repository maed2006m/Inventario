import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  FormGroupDirective,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { errorDeApi } from '../../../core/errores';
import { InventarioService } from '../../../core/inventario.service';
import { MotivoMovimiento, Producto, TipoMovimiento } from '../../../core/models';
import { BadgeEstadoComponent } from '../../../shared/badge-estado.component';
import { ETIQUETA_MOTIVO, motivosDe } from '../../../shared/etiquetas';

/**
 * Registrar movimiento (Ilustración 5, RF-14 a RF-22, RNF-01 y RNF-03). El formulario
 * valida en tiempo real contra la existencia para dar respuesta inmediata, pero la
 * decisión final siempre la toma el servidor: si responde 409 se muestra su causa y su
 * acción correctiva.
 */
@Component({
  selector: 'app-registrar-movimiento',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatSnackBarModule,
    BadgeEstadoComponent
  ],
  templateUrl: './registrar-movimiento.component.html',
  styleUrl: './registrar-movimiento.component.scss'
})
export class RegistrarMovimientoComponent {
  private fb = inject(FormBuilder);
  private api = inject(InventarioService);
  private snack = inject(MatSnackBar);

  productos = signal<Producto[]>([]);
  seleccionado = signal<Producto | null>(null);
  existencia = signal<number | null>(null);
  errorServidor = signal<{ message: string; accion: string } | null>(null);
  guardando = signal(false);
  etiqueta = ETIQUETA_MOTIVO;

  form = this.fb.nonNullable.group(
    {
      productoId: [0, [Validators.required, Validators.min(1)]],
      tipo: ['SALIDA' as TipoMovimiento, Validators.required],
      motivo: ['VENTA' as MotivoMovimiento, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
      observacion: ['', Validators.maxLength(250)]
    },
    { validators: [(g: AbstractControl) => this.salidaNoSuperaExistencia(g)] }
  );

  private tipo = toSignal(this.form.controls.tipo.valueChanges, { initialValue: this.form.controls.tipo.value });
  /** RN-04: COMPRA para entradas; VENTA, AVERIA o VENCIMIENTO para salidas. */
  motivos = computed(() => motivosDe(this.tipo()));

  constructor() {
    this.api.productos({ soloActivos: false }).subscribe((p) => this.productos.set(p));
    this.form.controls.tipo.valueChanges.subscribe((t) => {
      this.form.controls.motivo.setValue(motivosDe(t)[0]);
    });
    this.form.controls.productoId.valueChanges.subscribe((id) => this.alElegirProducto(id));
    const desdeRuta = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('productoId'));
    if (desdeRuta > 0) {
      this.form.controls.productoId.setValue(desdeRuta);
    }
  }

  /** Validador de grupo: una salida no puede superar la existencia conocida (RN-02, solo como ayuda). */
  salidaNoSuperaExistencia(g: AbstractControl): ValidationErrors | null {
    const ex = this.existencia ? this.existencia() : null;
    const v = g.value as { tipo: TipoMovimiento; cantidad: number };
    return v.tipo === 'SALIDA' && ex !== null && Number(v.cantidad) > ex ? { superaExistencia: { disponible: ex } } : null;
  }

  alElegirProducto(id: number): void {
    this.errorServidor.set(null);
    if (!id) {
      this.existencia.set(null);
      this.seleccionado.set(null);
      return;
    }
    this.api.producto(id).subscribe((p) => {
      this.seleccionado.set(p);
      this.existencia.set(p.existencia);
      this.form.updateValueAndValidity();
    });
  }

  guardar(directiva: FormGroupDirective): void {
    if (this.form.invalid) {
      return;
    }
    this.errorServidor.set(null);
    this.guardando.set(true);
    const v = this.form.getRawValue();
    this.api
      .registrarMovimiento({ ...v, cantidad: Number(v.cantidad), observacion: v.observacion || null })
      .subscribe({
        next: (r) => {
          this.guardando.set(false);
          this.snack.open(`Movimiento registrado. Existencia: ${r.existenciaResultante}`, 'OK', { duration: 3000 });
          const productoId = r.productoId;
          directiva.resetForm({ productoId, tipo: v.tipo, motivo: v.motivo, cantidad: 1, observacion: '' });
          this.alElegirProducto(productoId);
        },
        error: (e: HttpErrorResponse) => {
          this.guardando.set(false);
          this.errorServidor.set(errorDeApi(e));
        }
      });
  }
}
