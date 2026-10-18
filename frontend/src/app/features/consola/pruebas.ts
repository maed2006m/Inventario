import { MetodoHttp, RespuestaConsola } from '../../core/consola-api.service';

export type Credencial = 'admin' | 'tendero' | 'ninguna' | 'invalida';

/** Lo que cada prueba recibe: un cliente para pedir con cualquier credencial y datos compartidos entre pasos. */
export interface ContextoPrueba {
  pedir(
    cred: Credencial,
    metodo: MetodoHttp,
    ruta: string,
    opciones?: { query?: Record<string, unknown>; cuerpo?: unknown }
  ): Promise<RespuestaConsola>;
  /** Ids creados por pasos anteriores (proveedor, producto, movimiento). */
  datos: Record<string, number>;
}

export interface PruebaApi {
  grupo: string;
  nombre: string;
  referencia?: string;
  /** Lanza Error con la causa si el backend no se comporta como se espera. */
  ejecutar(ctx: ContextoPrueba): Promise<void>;
}

type Cuerpo = Record<string, unknown>;

/** Verifica el código de estado; si no coincide, falla mostrando el mensaje del ApiError. */
function esperar(r: RespuestaConsola, status: number): Cuerpo {
  if (r.status !== status) {
    const c = r.cuerpo as Cuerpo | null;
    const detalle = c && typeof c === 'object' && c['message'] ? `: ${c['message']}` : '';
    throw new Error(`Se esperaba ${status} y llegó ${r.status}${detalle}`);
  }
  return (r.cuerpo ?? {}) as Cuerpo;
}

function verificar(condicion: boolean, mensaje: string): void {
  if (!condicion) {
    throw new Error(mensaje);
  }
}

/** Un ApiError de negocio debe traer causa y acción correctiva (RNF-03). */
function conAccion(c: Cuerpo): void {
  verificar(!!c['message'] && !!c['accion'], 'El ApiError no trae "message" y "accion"');
}

function requiere(ctx: ContextoPrueba, clave: string): number {
  const id = ctx.datos[clave];
  verificar(id !== undefined, `Requiere que antes pase la prueba que crea "${clave}"`);
  return id;
}

const sufijo = () => String(Date.now()).slice(-7);

/**
 * Batería de pruebas de punta a punta contra el backend en ejecución. Las del grupo
 * "Flujo de inventario" crean un proveedor y un producto PRUEBA-xxxx; los movimientos
 * quedan registrados para siempre porque son inmutables (RNF-04).
 */
export const PRUEBAS: PruebaApi[] = [
  // ---- Seguridad ----
  {
    grupo: 'Seguridad',
    nombre: 'Login con contraseña incorrecta responde 401',
    referencia: 'RNF-09',
    async ejecutar(ctx) {
      esperar(
        await ctx.pedir('ninguna', 'POST', '/auth/login', { cuerpo: { username: 'admin', password: 'mala' } }),
        401
      );
    }
  },
  {
    grupo: 'Seguridad',
    nombre: 'Login sin datos responde 400 con errores por campo',
    async ejecutar(ctx) {
      const c = esperar(await ctx.pedir('ninguna', 'POST', '/auth/login', { cuerpo: {} }), 400);
      const campos = (c['campos'] ?? {}) as Cuerpo;
      verificar(!!campos['username'] && !!campos['password'], 'Faltan "campos.username" o "campos.password"');
    }
  },
  {
    grupo: 'Seguridad',
    nombre: 'Petición sin token responde 401',
    async ejecutar(ctx) {
      esperar(await ctx.pedir('ninguna', 'GET', '/productos'), 401);
    }
  },
  {
    grupo: 'Seguridad',
    nombre: 'Token alterado responde 401',
    async ejecutar(ctx) {
      esperar(await ctx.pedir('invalida', 'GET', '/productos'), 401);
    }
  },
  {
    grupo: 'Seguridad',
    nombre: '/auth/me devuelve el rol de cada token',
    async ejecutar(ctx) {
      const a = esperar(await ctx.pedir('admin', 'GET', '/auth/me'), 200);
      const t = esperar(await ctx.pedir('tendero', 'GET', '/auth/me'), 200);
      verificar(a['rol'] === 'ADMIN', `admin trae rol ${a['rol']}`);
      verificar(t['rol'] === 'TENDERO', `tendero trae rol ${t['rol']}`);
    }
  },
  {
    grupo: 'Seguridad',
    nombre: 'Tendero recibe 403 en las rutas solo-ADMIN',
    referencia: 'RNF-10',
    async ejecutar(ctx) {
      const rutas: [MetodoHttp, string, unknown?][] = [
        ['GET', '/reportes/rotacion'],
        ['GET', '/reportes/participacion'],
        ['GET', '/movimientos/export'],
        ['PUT', '/proveedores/1', {}],
        ['DELETE', '/proveedores/1']
      ];
      for (const [metodo, ruta, cuerpo] of rutas) {
        const r = await ctx.pedir('tendero', metodo, ruta, { cuerpo });
        verificar(r.status === 403, `${metodo} ${ruta} respondió ${r.status} en vez de 403`);
      }
    }
  },
  {
    grupo: 'Seguridad',
    nombre: 'Administrador accede a reportes y exportación CSV',
    referencia: 'HU12, HU13, HU15',
    async ejecutar(ctx) {
      esperar(await ctx.pedir('admin', 'GET', '/reportes/rotacion'), 200);
      const p = esperar(await ctx.pedir('admin', 'GET', '/reportes/participacion'), 200);
      verificar('indiceHhi' in p, 'La participación no trae "indiceHhi"');
      const csv = await ctx.pedir('admin', 'GET', '/movimientos/export');
      esperar(csv, 200);
      verificar(!!csv.contentDisposition, 'Falta el encabezado Content-Disposition (¿CORS no lo expone?)');
    }
  },

  // ---- Flujo de inventario ----
  {
    grupo: 'Flujo de inventario',
    nombre: 'Crear proveedor responde 201',
    referencia: 'HU1',
    async ejecutar(ctx) {
      const nit = '9' + sufijo() + '1';
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/proveedores', {
          cuerpo: { nombre: `Proveedor PRUEBA-${nit}`, nit, telefono: '3001234567', tiempoEntregaDias: 2 }
        }),
        201
      );
      ctx.datos['proveedor'] = c['id'] as number;
      ctx.datos['nit'] = Number(nit);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'NIT duplicado responde 409 con acción correctiva',
    referencia: 'RN-09',
    async ejecutar(ctx) {
      const nit = String(requiere(ctx, 'nit'));
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/proveedores', { cuerpo: { nombre: 'Duplicado', nit, tiempoEntregaDias: 1 } }),
        409
      );
      conAccion(c);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'NIT con formato inválido responde 400 en campos.nit',
    async ejecutar(ctx) {
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/proveedores', { cuerpo: { nombre: 'X', nit: 'abc', tiempoEntregaDias: 1 } }),
        400
      );
      verificar(!!(c['campos'] as Cuerpo | undefined)?.['nit'], 'Falta "campos.nit"');
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Crear producto: existencia 0 y estado AGOTADO',
    async ejecutar(ctx) {
      const proveedorId = requiere(ctx, 'proveedor');
      const sku = 'PRUEBA-' + sufijo();
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/productos', {
          cuerpo: {
            sku,
            nombre: `Producto ${sku}`,
            categoria: 'Pruebas',
            proveedorId,
            stockMinimo: 5,
            costoUnitario: 1000,
            precioVenta: 1500
          }
        }),
        201
      );
      verificar(c['existencia'] === 0, `existencia = ${c['existencia']}`);
      verificar(c['estado'] === 'AGOTADO', `estado = ${c['estado']}`);
      ctx.datos['producto'] = c['id'] as number;
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'SKU duplicado responde 409',
    referencia: 'RN-09',
    async ejecutar(ctx) {
      const id = requiere(ctx, 'producto');
      const p = esperar(await ctx.pedir('admin', 'GET', `/productos/${id}`), 200);
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/productos', {
          cuerpo: { ...p, id: undefined, nombre: 'Otro' }
        }),
        409
      );
      conAccion(c);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Entrada por compra de 10 unidades',
    referencia: 'RF-21',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/movimientos', {
          cuerpo: { productoId, tipo: 'ENTRADA', motivo: 'COMPRA', cantidad: 10, valorUnitario: 1000 }
        }),
        201
      );
      verificar(c['existenciaResultante'] === 10, `existenciaResultante = ${c['existenciaResultante']}`);
      ctx.datos['movimiento'] = c['id'] as number;
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Tipo y motivo incompatibles (ENTRADA + VENTA) responden 409',
    referencia: 'RN-04',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      conAccion(
        esperar(
          await ctx.pedir('admin', 'POST', '/movimientos', {
            cuerpo: { productoId, tipo: 'ENTRADA', motivo: 'VENTA', cantidad: 1 }
          }),
          409
        )
      );
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Cantidad 0 responde 400 en campos.cantidad',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      const c = esperar(
        await ctx.pedir('admin', 'POST', '/movimientos', {
          cuerpo: { productoId, tipo: 'SALIDA', motivo: 'VENTA', cantidad: 0 }
        }),
        400
      );
      verificar(!!(c['campos'] as Cuerpo | undefined)?.['cantidad'], 'Falta "campos.cantidad"');
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Tendero vende 7: queda en 3 y pasa a BAJO_STOCK',
    referencia: 'RF-22',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      const m = esperar(
        await ctx.pedir('tendero', 'POST', '/movimientos', {
          cuerpo: { productoId, tipo: 'SALIDA', motivo: 'VENTA', cantidad: 7 }
        }),
        201
      );
      verificar(m['existenciaResultante'] === 3, `existenciaResultante = ${m['existenciaResultante']}`);
      const p = esperar(await ctx.pedir('tendero', 'GET', `/productos/${productoId}`), 200);
      verificar(p['estado'] === 'BAJO_STOCK', `estado = ${p['estado']}`);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'El producto aparece en /productos/alertas',
    referencia: 'HU11',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      const lista = (await ctx.pedir('admin', 'GET', '/productos/alertas')).cuerpo as Cuerpo[];
      verificar(Array.isArray(lista) && lista.some((p) => p['id'] === productoId), 'No está en las alertas');
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Salida mayor que la existencia responde 409 y no cambia el stock',
    referencia: 'RN-02',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      conAccion(
        esperar(
          await ctx.pedir('admin', 'POST', '/movimientos', {
            cuerpo: { productoId, tipo: 'SALIDA', motivo: 'VENTA', cantidad: 100 }
          }),
          409
        )
      );
      const p = esperar(await ctx.pedir('admin', 'GET', `/productos/${productoId}`), 200);
      verificar(p['existencia'] === 3, `existencia = ${p['existencia']}`);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Editar o borrar un movimiento responde 405',
    referencia: 'RNF-04',
    async ejecutar(ctx) {
      const id = requiere(ctx, 'movimiento');
      for (const metodo of ['PUT', 'PATCH', 'DELETE'] as MetodoHttp[]) {
        const r = await ctx.pedir('admin', metodo, `/movimientos/${id}`, { cuerpo: { cantidad: 1 } });
        verificar(r.status === 405, `${metodo} respondió ${r.status} en vez de 405`);
      }
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Historial filtrado por producto trae los 2 movimientos',
    referencia: 'RF-11',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      const c = esperar(await ctx.pedir('admin', 'GET', '/movimientos', { query: { productoId } }), 200);
      verificar(c['totalElements'] === 2, `totalElements = ${c['totalElements']}`);
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Inactivar proveedor con productos activos responde 409',
    referencia: 'HU3',
    async ejecutar(ctx) {
      const id = requiere(ctx, 'proveedor');
      conAccion(esperar(await ctx.pedir('admin', 'DELETE', `/proveedores/${id}`), 409));
    }
  },
  {
    grupo: 'Flujo de inventario',
    nombre: 'Producto deshabilitado: rechaza venta, admite avería y se puede rehabilitar',
    referencia: 'RN-08',
    async ejecutar(ctx) {
      const productoId = requiere(ctx, 'producto');
      esperar(await ctx.pedir('admin', 'DELETE', `/productos/${productoId}`), 204);
      esperar(
        await ctx.pedir('admin', 'POST', '/movimientos', {
          cuerpo: { productoId, tipo: 'SALIDA', motivo: 'VENTA', cantidad: 1 }
        }),
        409
      );
      esperar(
        await ctx.pedir('admin', 'POST', '/movimientos', {
          cuerpo: { productoId, tipo: 'SALIDA', motivo: 'AVERIA', cantidad: 1, observacion: 'Prueba de consola' }
        }),
        201
      );
      const p = esperar(await ctx.pedir('admin', 'PATCH', `/productos/${productoId}/habilitar`), 200);
      verificar(p['activo'] === true && p['existencia'] === 2, `activo=${p['activo']}, existencia=${p['existencia']}`);
    }
  },

  // ---- Consultas y reportes ----
  {
    grupo: 'Consultas y reportes',
    nombre: 'Producto inexistente responde 404',
    async ejecutar(ctx) {
      conAccion(esperar(await ctx.pedir('admin', 'GET', '/productos/999999999'), 404));
    }
  },
  {
    grupo: 'Consultas y reportes',
    nombre: 'Rotación con fecha inicial posterior a la final responde 400',
    referencia: 'RN-15',
    async ejecutar(ctx) {
      conAccion(
        esperar(
          await ctx.pedir('admin', 'GET', '/reportes/rotacion', { query: { desde: '2026-02-01', hasta: '2026-01-01' } }),
          400
        )
      );
    }
  },
  {
    grupo: 'Consultas y reportes',
    nombre: 'Tablero devuelve todos sus indicadores',
    async ejecutar(ctx) {
      const c = esperar(await ctx.pedir('tendero', 'GET', '/dashboard/resumen'), 200);
      const faltan = ['productosActivos', 'productosBajoStock', 'productosAgotados', 'movimientosHoy', 'valorInventario', 'ventasDelMes'].filter(
        (k) => !(k in c)
      );
      verificar(faltan.length === 0, `Faltan: ${faltan.join(', ')}`);
    }
  }
];
