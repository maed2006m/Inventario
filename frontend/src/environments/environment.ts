/**
 * Entorno de desarrollo. La URL es relativa: `npm start` usa proxy.conf.json para
 * enviar /api al backend en http://localhost:8080 sin problemas de CORS.
 */
export const environment = {
  production: false,
  apiUrl: '/api'
};
