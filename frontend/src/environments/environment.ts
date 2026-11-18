/**
 * Entorno de desarrollo. La URL es relativa: `npm start` usa proxy.conf.json para
 * enviar /api al backend en http://localhost:8080 sin problemas de CORS.
 */
export const environment = {
  production: false,
  apiUrl: '/api',
  /** En desarrollo el login muestra los usuarios de demostración (V2__usuarios_iniciales.sql). */
  usuariosDemo: 'admin / Admin123* · tendero / Tendero123*' as string | null
};
