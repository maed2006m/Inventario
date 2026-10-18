import { MetodoHttp } from '../../core/consola-api.service';

/** Quién puede llamar el endpoint según SecurityConfig. */
export type Acceso = 'PUBLICO' | 'AUTENTICADO' | 'ADMIN';

/**
 * Un endpoint del backend con valores de ejemplo para la consola. Los segmentos `{id}` de la
 * ruta se reemplazan con `parametrosRuta`; `query` y `cuerpo` se editan como JSON.
 */
export interface EndpointConsola {
  grupo: string;
  metodo: MetodoHttp;
  ruta: string;
  descripcion: string;
  acceso: Acceso;
  referencia?: string;
  parametrosRuta?: Record<string, string>;
  query?: Record<string, unknown>;
  cuerpo?: unknown;
  /** Código esperado cuando la petición es válida (ej. 201, 204, 405). */
  esperado: number;
}

const hoy = () => new Date().toISOString().slice(0, 10);
const haceDias = (n: number) => new Date(Date.now() - n * 86_400_000).toISOString().slice(0, 10);

/** Todos los endpoints de los controladores (AuthController, Proveedor, Producto, Movimiento, Reporte). */
export function catalogoEndpoints(): EndpointConsola[] {
  return [
    // ---- Autenticación ----
    {
      grupo: 'Autenticación',
      metodo: 'POST',
      ruta: '/auth/login',
      descripcion: 'Inicia sesión y devuelve el JWT',
      acceso: 'PUBLICO',
      referencia: 'RNF-09',
      cuerpo: { username: 'admin', password: 'Admin123*' },
      esperado: 200
    },
    {
      grupo: 'Autenticación',
      metodo: 'GET',
      ruta: '/auth/me',
      descripcion: 'Usuario dueño del token',
      acceso: 'AUTENTICADO',
      esperado: 200
    },

    // ---- Proveedores ----
    {
      grupo: 'Proveedores',
      metodo: 'GET',
      ruta: '/proveedores',
      descripcion: 'Listar proveedores',
      acceso: 'AUTENTICADO',
      query: { soloActivos: false },
      esperado: 200
    },
    {
      grupo: 'Proveedores',
      metodo: 'GET',
      ruta: '/proveedores/{id}',
      descripcion: 'Obtener un proveedor',
      acceso: 'AUTENTICADO',
      parametrosRuta: { id: '1' },
      esperado: 200
    },
    {
      grupo: 'Proveedores',
      metodo: 'POST',
      ruta: '/proveedores',
      descripcion: 'Crear proveedor',
      acceso: 'AUTENTICADO',
      referencia: 'HU1',
      cuerpo: {
        nombre: 'Proveedor de prueba',
        nit: String(Date.now()).slice(-9),
        telefono: '3001234567',
        email: 'prueba@proveedor.co',
        direccion: 'Calle 1 # 2-3',
        tiempoEntregaDias: 2
      },
      esperado: 201
    },
    {
      grupo: 'Proveedores',
      metodo: 'PUT',
      ruta: '/proveedores/{id}',
      descripcion: 'Editar proveedor',
      acceso: 'ADMIN',
      referencia: 'HU2',
      parametrosRuta: { id: '1' },
      cuerpo: {
        nombre: 'Proveedor editado',
        nit: '900123456',
        telefono: '3001234567',
        email: 'editado@proveedor.co',
        direccion: 'Calle 1 # 2-3',
        tiempoEntregaDias: 3
      },
      esperado: 200
    },
    {
      grupo: 'Proveedores',
      metodo: 'DELETE',
      ruta: '/proveedores/{id}',
      descripcion: 'Inactivar proveedor (409 si tiene productos activos)',
      acceso: 'ADMIN',
      referencia: 'HU3',
      parametrosRuta: { id: '1' },
      esperado: 204
    },

    // ---- Productos ----
    {
      grupo: 'Productos',
      metodo: 'GET',
      ruta: '/productos',
      descripcion: 'Listar productos con filtros',
      acceso: 'AUTENTICADO',
      query: { q: '', proveedorId: '', categoria: '', estado: '', soloActivos: true },
      esperado: 200
    },
    {
      grupo: 'Productos',
      metodo: 'GET',
      ruta: '/productos/alertas',
      descripcion: 'Productos agotados o bajo stock',
      acceso: 'AUTENTICADO',
      esperado: 200
    },
    {
      grupo: 'Productos',
      metodo: 'GET',
      ruta: '/productos/{id}',
      descripcion: 'Obtener un producto con su existencia',
      acceso: 'AUTENTICADO',
      parametrosRuta: { id: '1' },
      esperado: 200
    },
    {
      grupo: 'Productos',
      metodo: 'POST',
      ruta: '/productos',
      descripcion: 'Crear producto',
      acceso: 'AUTENTICADO',
      cuerpo: {
        sku: 'PRUEBA-' + String(Date.now()).slice(-6),
        nombre: 'Producto de prueba',
        categoria: 'Pruebas',
        proveedorId: 1,
        stockMinimo: 5,
        costoUnitario: 1000,
        precioVenta: 1500
      },
      esperado: 201
    },
    {
      grupo: 'Productos',
      metodo: 'PUT',
      ruta: '/productos/{id}',
      descripcion: 'Editar producto',
      acceso: 'AUTENTICADO',
      parametrosRuta: { id: '1' },
      cuerpo: {
        sku: 'SKU-EXISTENTE',
        nombre: 'Producto editado',
        categoria: 'Pruebas',
        proveedorId: 1,
        stockMinimo: 5,
        costoUnitario: 1000,
        precioVenta: 1500
      },
      esperado: 200
    },
    {
      grupo: 'Productos',
      metodo: 'DELETE',
      ruta: '/productos/{id}',
      descripcion: 'Deshabilitar producto',
      acceso: 'AUTENTICADO',
      parametrosRuta: { id: '1' },
      esperado: 204
    },
    {
      grupo: 'Productos',
      metodo: 'PATCH',
      ruta: '/productos/{id}/habilitar',
      descripcion: 'Volver a habilitar un producto',
      acceso: 'AUTENTICADO',
      parametrosRuta: { id: '1' },
      esperado: 200
    },

    // ---- Movimientos ----
    {
      grupo: 'Movimientos',
      metodo: 'POST',
      ruta: '/movimientos',
      descripcion: 'Registrar entrada o salida',
      acceso: 'AUTENTICADO',
      referencia: 'RF-21',
      cuerpo: { productoId: 1, tipo: 'ENTRADA', motivo: 'COMPRA', cantidad: 10, valorUnitario: 1000, observacion: '' },
      esperado: 201
    },
    {
      grupo: 'Movimientos',
      metodo: 'GET',
      ruta: '/movimientos',
      descripcion: 'Historial paginado con filtros',
      acceso: 'AUTENTICADO',
      referencia: 'RF-11',
      query: { productoId: '', proveedorId: '', tipo: '', motivo: '', desde: '', hasta: '', page: 0, size: 20 },
      esperado: 200
    },
    {
      grupo: 'Movimientos',
      metodo: 'PUT',
      ruta: '/movimientos/{id}',
      descripcion: 'Editar movimiento: debe rechazarse (inmutable)',
      acceso: 'AUTENTICADO',
      referencia: 'RNF-04',
      parametrosRuta: { id: '1' },
      cuerpo: { cantidad: 1 },
      esperado: 405
    },
    {
      grupo: 'Movimientos',
      metodo: 'DELETE',
      ruta: '/movimientos/{id}',
      descripcion: 'Borrar movimiento: debe rechazarse (inmutable)',
      acceso: 'AUTENTICADO',
      referencia: 'RNF-04',
      parametrosRuta: { id: '1' },
      esperado: 405
    },
    {
      grupo: 'Movimientos',
      metodo: 'GET',
      ruta: '/movimientos/export',
      descripcion: 'Exportar historial a CSV',
      acceso: 'ADMIN',
      referencia: 'HU15',
      query: { productoId: '', tipo: '', motivo: '', desde: '', hasta: '' },
      esperado: 200
    },

    // ---- Reportes y tablero ----
    {
      grupo: 'Reportes',
      metodo: 'GET',
      ruta: '/dashboard/resumen',
      descripcion: 'Indicadores del tablero',
      acceso: 'AUTENTICADO',
      esperado: 200
    },
    {
      grupo: 'Reportes',
      metodo: 'GET',
      ruta: '/reportes/rotacion',
      descripcion: 'Ranking de rotación por periodo',
      acceso: 'ADMIN',
      referencia: 'HU12',
      query: { desde: haceDias(30), hasta: hoy() },
      esperado: 200
    },
    {
      grupo: 'Reportes',
      metodo: 'GET',
      ruta: '/reportes/participacion',
      descripcion: 'Participación por proveedor e índice HHI',
      acceso: 'ADMIN',
      referencia: 'HU13',
      esperado: 200
    }
  ];
}
