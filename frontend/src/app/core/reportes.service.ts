import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api.config';
import { aParams } from './inventario.service';
import { Participacion, RotacionItem } from './models';

/** Reportes de la Épica 3 (HU12, HU13). Solo para el rol ADMIN. */
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private http = inject(HttpClient);

  /** Ranking de rotación (RF-25). Fechas AAAA-MM-DD; por defecto el backend usa los últimos 30 días. */
  rotacion(desde?: string, hasta?: string): Observable<RotacionItem[]> {
    return this.http.get<RotacionItem[]>(`${API}/reportes/rotacion`, { params: aParams({ desde, hasta }) });
  }

  /** Participación de cada proveedor en el valor del inventario y su índice HHI (RF-26). */
  participacion(): Observable<Participacion> {
    return this.http.get<Participacion>(`${API}/reportes/participacion`);
  }
}
