import { Routes } from '@angular/router';
import { ReportesShellComponent } from './reportes-shell.component';

/** Reportes de la Épica 3 y analítica avanzada. La ruta padre ya exige rol ADMIN. */
export const REPORTES_ROUTES: Routes = [
  {
    path: '',
    component: ReportesShellComponent,
    children: [
      { path: 'rotacion', loadComponent: () => import('./rotacion.component').then((m) => m.RotacionComponent) },
      {
        path: 'participacion',
        loadComponent: () => import('./participacion.component').then((m) => m.ParticipacionComponent)
      },
      { path: '', pathMatch: 'full', redirectTo: 'rotacion' }
    ]
  }
];
