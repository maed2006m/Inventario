import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from './api.config';
import { Usuario, UsuarioRequest } from './models';

/** Gestión de cuentas (RF-30). Solo para el rol ADMIN; el backend responde 403 a los demás. */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${API}/usuarios`);
  }

  crear(u: UsuarioRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API}/usuarios`, u);
  }

  desactivar(id: number): Observable<Usuario> {
    return this.http.put<Usuario>(`${API}/usuarios/${id}/desactivar`, null);
  }

  activar(id: number): Observable<Usuario> {
    return this.http.put<Usuario>(`${API}/usuarios/${id}/activar`, null);
  }
}
