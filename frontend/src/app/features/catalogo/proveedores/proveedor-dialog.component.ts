import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { errorDeApi } from '../../../core/errores';
import { InventarioService } from '../../../core/inventario.service';
import { Proveedor } from '../../../core/models';

/** Formulario modal para crear (HU1) o editar (HU2) un proveedor. */
@Component({
  selector: 'app-proveedor-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos ? 'Editar proveedor' : 'Nuevo proveedor' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="campos tactil">
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="120" />
          <mat-error>El nombre es obligatorio</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>NIT</mat-label>
          <input matInput formControlName="nit" placeholder="900100001-1" />
          <mat-error>Entre 6 y 12 dígitos y opcionalmente el dígito de verificación</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Teléfono</mat-label>
          <input matInput formControlName="telefono" inputmode="numeric" />
          <mat-error>Entre 7 y 15 dígitos</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Correo</mat-label>
          <input matInput formControlName="email" type="email" />
          <mat-error>Correo no válido</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Dirección</mat-label>
          <input matInput formControlName="direccion" maxlength="200" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Tiempo de entrega (días)</mat-label>
          <input matInput type="number" min="0" formControlName="tiempoEntregaDias" />
          <mat-hint>Se usa para calcular el punto de reorden</mat-hint>
        </mat-form-field>
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
  styles: [`.campos { display: grid; gap: 4px; min-width: min(420px, 80vw); }`]
})
export class ProveedorDialogComponent {
  datos = inject<Proveedor | null>(MAT_DIALOG_DATA);
  private ref = inject<MatDialogRef<ProveedorDialogComponent>>(MatDialogRef);
  private api = inject(InventarioService);
  private fb = inject(FormBuilder);

  guardando = signal(false);
  error = signal<{ message: string; accion: string } | null>(null);

  form = this.fb.group({
    nombre: [this.datos?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    nit: [this.datos?.nit ?? '', [Validators.required, Validators.pattern(/^\d{6,12}-?\d?$/)]],
    telefono: [this.datos?.telefono ?? '', Validators.pattern(/^\d{7,15}$/)],
    email: [this.datos?.email ?? '', Validators.email],
    direccion: [this.datos?.direccion ?? ''],
    tiempoEntregaDias: [this.datos?.tiempoEntregaDias ?? 2, [Validators.required, Validators.min(0)]]
  });

  guardar(): void {
    const v = this.form.getRawValue();
    const cuerpo = {
      nombre: v.nombre ?? '',
      nit: v.nit ?? '',
      telefono: v.telefono || null,
      email: v.email || null,
      direccion: v.direccion || null,
      tiempoEntregaDias: Number(v.tiempoEntregaDias ?? 0)
    };
    this.guardando.set(true);
    const peticion = this.datos ? this.api.editarProveedor(this.datos.id, cuerpo) : this.api.crearProveedor(cuerpo);
    peticion.subscribe({
      next: (p) => this.ref.close(p),
      error: (e) => {
        this.guardando.set(false);
        this.error.set(errorDeApi(e));
      }
    });
  }
}
