import { KeyValuePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ConsolaApiService, MetodoHttp, RespuestaConsola } from '../../core/consola-api.service';
import { EndpointConsola, catalogoEndpoints } from './endpoints';
import { ContextoPrueba, Credencial, PRUEBAS, PruebaApi } from './pruebas';

/** En el explorador también se puede usar el token de la sesión con la que se entró a la app. */
type CredencialExplorador = Credencial | 'sesion';

type EstadoPrueba = 'pendiente' | 'ejecutando' | 'ok' | 'falla';

interface ResultadoPrueba {
  prueba: PruebaApi;
  estado: EstadoPrueba;
  mensaje?: string;
  ms?: number;
}

/** JWT con firma falsa: el backend debe tratarlo como anónimo y responder 401. */
const TOKEN_INVALIDO = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbCI6IkFETUlOIn0.firma-falsa';

/**
 * Consola para probar el backend completo desde el navegador: un explorador para enviar
 * cualquier endpoint con cualquier credencial y ver la respuesta cruda, y una batería de
 * pruebas automáticas que verifica reglas de negocio, validaciones y la matriz de roles.
 */
@Component({
  selector: 'app-consola',
  standalone: true,
  imports: [
    FormsModule,
    KeyValuePipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTabsModule
  ],
  templateUrl: './consola.component.html',
  styleUrl: './consola.component.scss'
})
export class ConsolaComponent {
  private api = inject(ConsolaApiService);
  private auth = inject(AuthService);

  // ---- Credenciales de prueba (usuarios sembrados por V2) ----
  usuarios = {
    admin: { username: 'admin', password: 'Admin123*' },
    tendero: { username: 'tendero', password: 'Tendero123*' }
  };
  tokenAdmin = signal<string | null>(null);
  tokenTendero = signal<string | null>(null);
  errorTokens = signal<string | null>(null);

  // ---- Explorador ----
  endpoints = catalogoEndpoints();
  grupos = [...new Set(this.endpoints.map((e) => e.grupo))];
  seleccionado = signal<EndpointConsola>(this.endpoints[0]);
  credencial: CredencialExplorador = 'sesion';
  parametrosRuta: Record<string, string> = {};
  queryTexto = '';
  cuerpoTexto = '';
  errorJson = signal<string | null>(null);
  enviando = signal(false);
  respuesta = signal<RespuestaConsola | null>(null);

  // ---- Pruebas automáticas ----
  resultados = signal<ResultadoPrueba[]>(PRUEBAS.map((prueba) => ({ prueba, estado: 'pendiente' as const })));
  ejecutandoPruebas = signal(false);
  resumen = computed(() => {
    const r = this.resultados();
    return {
      total: r.length,
      ok: r.filter((x) => x.estado === 'ok').length,
      fallas: r.filter((x) => x.estado === 'falla').length
    };
  });
  gruposPruebas = [...new Set(PRUEBAS.map((p) => p.grupo))];

  constructor() {
    this.seleccionar(this.endpoints[0]);
  }

  // ---------------------------------------------------------------- Credenciales

  /** Inicia sesión con los dos usuarios de prueba y guarda sus tokens. */
  async obtenerTokens(): Promise<boolean> {
    this.errorTokens.set(null);
    const [a, t] = await Promise.all([this.login(this.usuarios.admin), this.login(this.usuarios.tendero)]);
    this.tokenAdmin.set(a);
    this.tokenTendero.set(t);
    if (!a || !t) {
      this.errorTokens.set(
        `No se pudo iniciar sesión como ${[!a && 'administrador', !t && 'tendero'].filter(Boolean).join(' ni ')}. ` +
          'Revise usuario/contraseña y que el backend esté en ejecución.'
      );
    }
    return !!a && !!t;
  }

  private async login(u: { username: string; password: string }): Promise<string | null> {
    const r = await firstValueFrom(this.api.enviar({ metodo: 'POST', ruta: '/auth/login', cuerpo: u }));
    return r.ok ? ((r.cuerpo as { token?: string })?.token ?? null) : null;
  }

  private token(c: CredencialExplorador): string | null {
    switch (c) {
      case 'sesion':
        return this.auth.token();
      case 'admin':
        return this.tokenAdmin();
      case 'tendero':
        return this.tokenTendero();
      case 'invalida':
        return TOKEN_INVALIDO;
      default:
        return null;
    }
  }

  // ---------------------------------------------------------------- Explorador

  endpointsDe(grupo: string): EndpointConsola[] {
    return this.endpoints.filter((e) => e.grupo === grupo);
  }

  seleccionar(e: EndpointConsola): void {
    this.seleccionado.set(e);
    this.parametrosRuta = { ...(e.parametrosRuta ?? {}) };
    this.queryTexto = e.query ? JSON.stringify(e.query, null, 2) : '';
    this.cuerpoTexto = e.cuerpo !== undefined ? JSON.stringify(e.cuerpo, null, 2) : '';
    this.errorJson.set(null);
    this.respuesta.set(null);
    if (e.acceso === 'PUBLICO') {
      this.credencial = 'ninguna';
    } else if (this.credencial === 'ninguna') {
      this.credencial = 'sesion';
    }
  }

  /** Ruta con los {parámetros} reemplazados, tal como se enviará. */
  rutaFinal(): string {
    return this.seleccionado().ruta.replace(/\{(\w+)\}/g, (_, k: string) =>
      encodeURIComponent(this.parametrosRuta[k] ?? '')
    );
  }

  admiteCuerpo(m: MetodoHttp): boolean {
    return m !== 'GET' && m !== 'DELETE';
  }

  async enviar(): Promise<void> {
    const e = this.seleccionado();
    let query: Record<string, unknown> | undefined;
    let cuerpo: unknown;
    try {
      query = this.queryTexto.trim() ? JSON.parse(this.queryTexto) : undefined;
      cuerpo = this.admiteCuerpo(e.metodo) && this.cuerpoTexto.trim() ? JSON.parse(this.cuerpoTexto) : undefined;
    } catch (err) {
      this.errorJson.set(`JSON inválido: ${(err as Error).message}`);
      return;
    }
    this.errorJson.set(null);
    this.enviando.set(true);
    try {
      const r = await firstValueFrom(
        this.api.enviar({ metodo: e.metodo, ruta: this.rutaFinal(), query, cuerpo, token: this.token(this.credencial) })
      );
      this.respuesta.set(r);
      // Un login exitoso desde el explorador también actualiza el token del rol correspondiente.
      if (e.ruta === '/auth/login' && r.ok) {
        const c = r.cuerpo as { token: string; rol: string };
        (c.rol === 'ADMIN' ? this.tokenAdmin : this.tokenTendero).set(c.token);
      }
    } finally {
      this.enviando.set(false);
    }
  }

  /** Texto del cuerpo para mostrar: JSON indentado o el texto tal cual (CSV). */
  cuerpoLegible(r: RespuestaConsola): string {
    if (r.cuerpo === null) {
      return '(sin cuerpo)';
    }
    return typeof r.cuerpo === 'string' ? r.cuerpo : JSON.stringify(r.cuerpo, null, 2);
  }

  // ---------------------------------------------------------------- Pruebas automáticas

  icono(estado: EstadoPrueba): string {
    return { ok: 'check_circle', falla: 'cancel', ejecutando: 'hourglass_top', pendiente: 'radio_button_unchecked' }[
      estado
    ];
  }

  resultadosDe(grupo: string): ResultadoPrueba[] {
    return this.resultados().filter((r) => r.prueba.grupo === grupo);
  }

  async ejecutarPruebas(): Promise<void> {
    this.ejecutandoPruebas.set(true);
    this.resultados.set(PRUEBAS.map((prueba) => ({ prueba, estado: 'pendiente' as const })));
    try {
      if (!(await this.obtenerTokens())) {
        return;
      }
      const ctx: ContextoPrueba = {
        datos: {},
        pedir: (cred, metodo, ruta, op) =>
          firstValueFrom(
            this.api.enviar({ metodo, ruta, query: op?.query, cuerpo: op?.cuerpo, token: this.token(cred) })
          )
      };
      // En orden: las pruebas del flujo dependen de los datos que crean las anteriores.
      for (const prueba of PRUEBAS) {
        this.actualizar(prueba, { estado: 'ejecutando' });
        const inicio = performance.now();
        try {
          await prueba.ejecutar(ctx);
          this.actualizar(prueba, { estado: 'ok', ms: Math.round(performance.now() - inicio) });
        } catch (err) {
          this.actualizar(prueba, {
            estado: 'falla',
            mensaje: (err as Error).message,
            ms: Math.round(performance.now() - inicio)
          });
        }
      }
    } finally {
      this.ejecutandoPruebas.set(false);
    }
  }

  private actualizar(prueba: PruebaApi, cambio: Partial<ResultadoPrueba>): void {
    this.resultados.update((lista) => lista.map((r) => (r.prueba === prueba ? { ...r, ...cambio } : r)));
  }
}
