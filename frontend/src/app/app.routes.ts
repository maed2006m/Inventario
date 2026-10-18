import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/guards';
import { ShellComponent } from './layout/shell.component';

/** Rutas con control por rol (RNF-09, RNF-10). El menú del Administrador agrega Reportes. */
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    component: ShellComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'productos',
        loadComponent: () =>
          import('./features/catalogo/productos/productos.component').then((m) => m.ProductosComponent)
      },
      {
        path: 'proveedores',
        loadComponent: () =>
          import('./features/catalogo/proveedores/proveedores.component').then((m) => m.ProveedoresComponent)
      },
      {
        path: 'movimientos/nuevo',
        loadComponent: () =>
          import('./features/inventario/registrar-movimiento/registrar-movimiento.component').then(
            (m) => m.RegistrarMovimientoComponent
          )
      },
      {
        path: 'movimientos',
        loadComponent: () =>
          import('./features/inventario/historial/historial.component').then((m) => m.HistorialComponent)
      },
      {
        path: 'alertas',
        loadComponent: () => import('./features/inventario/alertas/alertas.component').then((m) => m.AlertasComponent)
      },
      {
        path: 'reportes',
        canActivate: [rolGuard('ADMIN')],
        loadChildren: () => import('./features/reportes/reportes.routes').then((m) => m.REPORTES_ROUTES)
      },
      {
        path: 'consola',
        loadComponent: () => import('./features/consola/consola.component').then((m) => m.ConsolaComponent)
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: '' }
];
