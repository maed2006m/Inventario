import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../core/auth.service';

interface Opcion {
  ruta: string;
  texto: string;
  icono: string;
  soloAdmin?: boolean;
}

/**
 * Estructura principal: barra lateral fija en pantallas de 960 px o más y cajón
 * desplegable en tableta y teléfono (RNF-02).
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  template: `
    <mat-sidenav-container class="contenedor">
      <mat-sidenav
        #nav
        [mode]="escritorio() ? 'side' : 'over'"
        [opened]="escritorio()"
        class="lateral"
        [attr.role]="'navigation'"
      >
        <div class="marca">
          <mat-icon>storefront</mat-icon>
          <span>Mi Tienda</span>
        </div>
        <mat-nav-list>
          @for (o of opciones(); track o.ruta) {
            <a
              mat-list-item
              [routerLink]="o.ruta"
              routerLinkActive="activo"
              [routerLinkActiveOptions]="{ exact: o.ruta === '/movimientos' }"
              (click)="!escritorio() && nav.close()"
            >
              <mat-icon matListItemIcon>{{ o.icono }}</mat-icon>
              <span matListItemTitle>{{ o.texto }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar class="cabecera">
          @if (!escritorio()) {
            <button mat-icon-button (click)="nav.toggle()" aria-label="Abrir menú"><mat-icon>menu</mat-icon></button>
          }
          <span class="titulo">Inventario Tienda de Barrio</span>
          <span class="espacio"></span>
          <span class="usuario ocultar-movil">{{ auth.sesion()?.nombre }} · {{ auth.esAdmin() ? 'Administrador' : 'Tendero' }}</span>
          <button mat-icon-button (click)="auth.logout()" matTooltip="Cerrar sesión" aria-label="Cerrar sesión">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>
        <main><router-outlet /></main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [
    `
      .contenedor { height: 100vh; }
      .lateral { width: 232px; background: #fff; border-right: 1px solid var(--borde); }
      .marca { display: flex; align-items: center; gap: 8px; padding: 18px 16px; font-weight: 500; font-size: 1.1rem;
        color: var(--verde); }
      .activo { background: var(--verde-claro) !important; }
      .cabecera { position: sticky; top: 0; z-index: 2; background: #fff; border-bottom: 1px solid var(--borde); gap: 8px; }
      .titulo { font-size: 1rem; font-weight: 500; }
      .espacio { flex: 1; }
      .usuario { font-size: 0.85rem; color: var(--gris); }
    `
  ]
})
export class ShellComponent {
  auth = inject(AuthService);
  escritorio = toSignal(inject(BreakpointObserver).observe('(min-width: 960px)').pipe(map((r) => r.matches)), {
    initialValue: true
  });

  private todas: Opcion[] = [
    { ruta: '/dashboard', texto: 'Tablero', icono: 'dashboard' },
    { ruta: '/movimientos/nuevo', texto: 'Registrar movimiento', icono: 'add_circle' },
    { ruta: '/productos', texto: 'Productos', icono: 'inventory_2' },
    { ruta: '/proveedores', texto: 'Proveedores', icono: 'local_shipping' },
    { ruta: '/movimientos', texto: 'Historial', icono: 'history' },
    { ruta: '/alertas', texto: 'Alertas de stock', icono: 'notifications_active' },
    { ruta: '/reportes', texto: 'Reportes', icono: 'insights', soloAdmin: true }
  ];

  opciones = () => this.todas.filter((o) => !o.soloAdmin || this.auth.esAdmin());
}
