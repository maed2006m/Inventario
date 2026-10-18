import { HttpBackend, HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { API } from './api.config';
import { aParams } from './inventario.service';

export type MetodoHttp = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/** Petición cruda que arma la consola de pruebas. `ruta` es relativa a API (ej. "/productos/1"). */
export interface PeticionConsola {
  metodo: MetodoHttp;
  ruta: string;
  query?: Record<string, unknown>;
  cuerpo?: unknown;
  token?: string | null;
}

/** Respuesta tal como llegó: código, tiempo, encabezados útiles y cuerpo (JSON si se puede, si no texto). */
export interface RespuestaConsola {
  status: number;
  ok: boolean;
  ms: number;
  url: string;
  contentType: string | null;
  contentDisposition: string | null;
  cuerpo: unknown;
}

/**
 * Cliente HTTP de la consola de pruebas. Se construye sobre HttpBackend para NO pasar por
 * authInterceptor: así un 401 provocado a propósito no cierra la sesión real, y cada
 * petición elige su propio token (Administrador, Tendero o ninguno) para probar la
 * matriz de permisos de SecurityConfig (RNF-09, RNF-10).
 */
@Injectable({ providedIn: 'root' })
export class ConsolaApiService {
  private http = new HttpClient(inject(HttpBackend));

  /** Envía la petición y nunca falla: los errores HTTP se devuelven como respuesta normal. */
  enviar(p: PeticionConsola): Observable<RespuestaConsola> {
    const url = `${API}${p.ruta}`;
    const inicio = performance.now();
    let headers = new HttpHeaders();
    if (p.token) {
      headers = headers.set('Authorization', `Bearer ${p.token}`);
    }
    const tieneCuerpo = p.cuerpo !== undefined && p.cuerpo !== null && p.metodo !== 'GET';

    return this.http
      .request(p.metodo, url, {
        headers,
        params: aParams(p.query ?? {}),
        body: tieneCuerpo ? p.cuerpo : null,
        observe: 'response',
        responseType: 'text'
      })
      .pipe(
        map((r: HttpResponse<string>) => this.aRespuesta(r.status, r.headers, r.body, url, inicio)),
        catchError((e: HttpErrorResponse) =>
          of(this.aRespuesta(e.status, e.headers, typeof e.error === 'string' ? e.error : null, url, inicio))
        )
      );
  }

  private aRespuesta(
    status: number,
    headers: HttpHeaders | undefined,
    texto: string | null,
    url: string,
    inicio: number
  ): RespuestaConsola {
    return {
      status,
      ok: status >= 200 && status < 300,
      ms: Math.round(performance.now() - inicio),
      url,
      contentType: headers?.get('Content-Type') ?? null,
      contentDisposition: headers?.get('Content-Disposition') ?? null,
      cuerpo: ConsolaApiService.interpretar(texto)
    };
  }

  /** JSON si el texto lo es; si no (CSV, vacío), el texto tal cual. */
  static interpretar(texto: string | null): unknown {
    if (texto === null || texto === '') {
      return null;
    }
    try {
      return JSON.parse(texto);
    } catch {
      return texto;
    }
  }
}
