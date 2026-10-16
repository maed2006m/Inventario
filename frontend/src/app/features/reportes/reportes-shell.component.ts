import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** Pestañas de reportes (Ilustración 8). */
@Component({
  selector: 'app-reportes-shell',
  standalone: true,
  imports: [MatTabsModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="pagina">
      <h1>Reportes</h1>
      <nav mat-tab-nav-bar [tabPanel]="panel">
        @for (t of pestanas; track t.ruta) {
          <a mat-tab-link [routerLink]="t.ruta" routerLinkActive #rla="routerLinkActive" [active]="rla.isActive">{{ t.texto }}</a>
        }
      </nav>
      <mat-tab-nav-panel #panel><router-outlet /></mat-tab-nav-panel>
    </div>
  `
})
export class ReportesShellComponent {
  pestanas = [
    { ruta: 'rotacion', texto: 'Rotación' },
    { ruta: 'participacion', texto: 'Participación por proveedor' }
  ];
}
