import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { errorDeApi } from '../../../core/errores';
import { InventarioService } from '../../../core/inventario.service';
import { Producto, Proveedor } from '../../../core/models';

export interface DatosProductoDialog {
  producto: Producto | null;
  proveedores: Proveedor[];
}

/**
 * Crear (HU4) o editar (HU5) un producto. No hay campo de existencia: la existencia
 * siempre se calcula a partir de los movimientos (RN-01).
 */
@Component({
  selector: 'app-producto-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.producto ? 'Editar producto' : 'Nuevo producto' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos tactil">
        <mat-form-field appearance="outline">
          <mat-label>SKU</mat-label>
          <input matInput formControlName="sku" maxlength="30" />
          <mat-hint>No se puede cambiar si el producto ya tiene movimientos</mat-hint>
          <mat-error>El SKU es obligatorio</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="120" />
          <mat-error>El nombre es obligatorio</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Categoría</mat-label>
          <input matInput formControlName="categoria" maxlength="60" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Proveedor</mat-label>
          <mat-select formControlName="proveedorId">
            @for (p of proveedoresActivos; track p.id) {
              <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
            }
          </mat-select>
          <mat-error>Elija un proveedor activo</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Stock mínimo</mat-label>
          <input matInput type="number" min="0" formControlName="stockMinimo" />
          <mat-hint>Por debajo o igual a este valor el producto pasa a "Bajo stock"</mat-hint>
        </mat-form-field>
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Costo unitario</mat-label>
            <input matInput type="number" min="0" formControlName="costoUnitario" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Precio de venta</mat-label>
            <input matInput type="number" min="0" formControlName="precioVenta" />
          </mat-form-field>
        </div>
        @if (error(); as e) {
          <div class="error-servidor" role="alert"><strong>{{ e.message }}</strong>{{ e.accion }}</div>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [
    `
      .campos { display: grid; gap: 4px; min-width: min(460px, 80vw); }
      .fila { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    `
  ]
})
export class ProductoDialogComponent {
  datos = inject<DatosProductoDialog>(MAT_DIALOG_DATA);
  private ref = inject<MatDialogRef<ProductoDialogComponent>>(MatDialogRef);
  private api = inject(InventarioService);
  private fb = inject(FormBuilder);

  guardando = signal(false);
  error = signal<{ message: string; accion: string } | null>(null);

  /** RN-06: solo se ofrecen proveedores activos (más el actual, si se edita). */
  proveedoresActivos = this.datos.proveedores.filter((p) => p.activo || p.id === this.datos.producto?.proveedorId);

  private p = this.datos.producto;
  form = this.fb.nonNullable.group({
    sku: [this.p?.sku ?? '', [Validators.required, Validators.maxLength(30)]],
    nombre: [this.p?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    categoria: [this.p?.categoria ?? ''],
    proveedorId: [this.p?.proveedorId ?? 0, [Validators.required, Validators.min(1)]],
    stockMinimo: [this.p?.stockMinimo ?? 0, [Validators.required, Validators.min(0)]],
    costoUnitario: [this.p?.costoUnitario ?? 0, [Validators.required, Validators.min(0)]],
    precioVenta: [this.p?.precioVenta ?? 0, [Validators.required, Validators.min(0)]]
  });

  guardar(): void {
    const v = this.form.getRawValue();
    const cuerpo = {
      ...v,
      categoria: v.categoria || null,
      stockMinimo: Number(v.stockMinimo),
      costoUnitario: Number(v.costoUnitario),
      precioVenta: Number(v.precioVenta)
    };
    this.guardando.set(true);
    const peticion = this.p ? this.api.editarProducto(this.p.id, cuerpo) : this.api.crearProducto(cuerpo);
    peticion.subscribe({
      next: (r) => this.ref.close(r),
      error: (e) => {
        this.guardando.set(false);
        this.error.set(errorDeApi(e));
      }
    });
  }
}
