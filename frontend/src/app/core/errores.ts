import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

/** Extrae causa y acción correctiva de un error HTTP (RNF-03). */
export function errorDeApi(e: unknown): { message: string; accion: string; campos?: Record<string, string> | null } {
  if (e instanceof HttpErrorResponse) {
    const cuerpo = e.error as Partial<ApiError> | null;
    if (cuerpo && typeof cuerpo === 'object' && cuerpo.message) {
      return { message: cuerpo.message, accion: cuerpo.accion ?? '', campos: cuerpo.campos };
    }
    if (e.status === 0) {
      return { message: 'No hay conexión con el servidor.', accion: 'Verifique que el backend esté en ejecución.' };
    }
    return { message: `Error ${e.status}.`, accion: 'Intente de nuevo.' };
  }
  return { message: 'Error inesperado.', accion: 'Intente de nuevo.' };
}
