import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api.config';
import {
  Dashboard,
  FiltrosMovimiento,
  FiltrosProducto,
  Movimiento,
  MovimientoRequest,
  Pagina,
  Producto,
  ProductoRequest,
  Proveedor,
  ProveedorRequest
} from './models';

/** Convierte un objeto de filtros en parámetros HTTP, omitiendo vacíos. */
export function aParams(filtros: object): HttpParams {
  let p = new HttpParams();
  Object.entries(filtros).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') {
      p = p.set(k, String(v));
    }
  });
  return p;
}

/** Catálogo, existencias y movimientos (Épicas 1 y 2). Las llamadas HTTP viven solo en servicios. */
@Injectable({ providedIn: 'root' })
export class InventarioService {
  private http = inject(HttpClient);

  // ---- Proveedores ----
  proveedores(soloActivos = false): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${API}/proveedores`, { params: aParams({ soloActivos }) });
  }
  crearProveedor(p: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(`${API}/proveedores`, p);
  }
  editarProveedor(id: number, p: ProveedorRequest): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${API}/proveedores/${id}`, p);
  }
  inactivarProveedor(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/proveedores/${id}`);
  }

  // ---- Productos ----
  productos(f: FiltrosProducto = {}): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${API}/productos`, { params: aParams(f) });
  }
  producto(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${API}/productos/${id}`);
  }
  alertas(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${API}/productos/alertas`);
  }
  crearProducto(p: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(`${API}/productos`, p);
  }
  editarProducto(id: number, p: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${API}/productos/${id}`, p);
  }
  deshabilitarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/productos/${id}`);
  }
  habilitarProducto(id: number): Observable<Producto> {
    return this.http.patch<Producto>(`${API}/productos/${id}/habilitar`, {});
  }
  /** Movimientos de un producto: usa el histórico filtrado por producto (RF-11). */
  movimientosDeProducto(id: number, page = 0, size = 10): Observable<Pagina<Movimiento>> {
    return this.http.get<Pagina<Movimiento>>(`${API}/movimientos`, {
      params: aParams({ productoId: id, page, size, sort: 'fecha,desc' })
    });
  }

  // ---- Movimientos ----
  registrarMovimiento(m: MovimientoRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${API}/movimientos`, m);
  }
  historial(f: FiltrosMovimiento, page: number, size: number): Observable<Pagina<Movimiento>> {
    return this.http.get<Pagina<Movimiento>>(`${API}/movimientos`, {
      params: aParams({ ...f, page, size, sort: 'fecha,desc' })
    });
  }
  /** Descarga el CSV con eventos de progreso para no bloquear la interfaz (RNF-07). */
  exportarCsv(f: FiltrosMovimiento): Observable<HttpEvent<Blob>> {
    return this.http.get(`${API}/movimientos/export`, {
      params: aParams(f),
      responseType: 'blob',
      observe: 'events',
      reportProgress: true
    });
  }

  // ---- Tablero ----
  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${API}/dashboard/resumen`);
  }
}
