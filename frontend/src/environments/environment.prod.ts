/**
 * Entorno de producción (Entrega 3). Nginx sirve el frontend y reenvía /api al backend
 * desde el mismo dominio, por eso la URL sigue siendo relativa.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
  /**
   * En producción las contraseñas son otras: no se muestran los usuarios de demostración.
   * El texto vive solo en environment.ts para que ni siquiera llegue al build de producción.
   */
  usuariosDemo: null as string | null
};
