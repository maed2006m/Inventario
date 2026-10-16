/** Tipos que reflejan los DTO del backend (sección 7 de la guía). */

export type Rol = 'TENDERO' | 'ADMIN';
export type EstadoStock = 'EN_STOCK' | 'BAJO_STOCK' | 'AGOTADO';
export type TipoMovimiento = 'ENTRADA' | 'SALIDA';
export type MotivoMovimiento = 'COMPRA' | 'VENTA' | 'AVERIA' | 'VENCIMIENTO';

export interface LoginResponse {
  token: string;
  username: string;
  nombre: string;
  rol: Rol;
  expiraEnMinutos: number;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  accion: string;
  path?: string;
  campos?: Record<string, string> | null;
}

export interface Proveedor {
  id: number;
  nombre: string;
  nit: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  tiempoEntregaDias: number;
  activo: boolean;
  productosActivos: number;
}

export type ProveedorRequest = Omit<Proveedor, 'id' | 'activo' | 'productosActivos'>;

export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  categoria: string | null;
  proveedorId: number;
  proveedorNombre: string;
  stockMinimo: number;
  costoUnitario: number;
  precioVenta: number;
  activo: boolean;
  existencia: number;
  estado: EstadoStock;
}

export interface ProductoRequest {
  sku: string;
  nombre: string;
  categoria: string | null;
  proveedorId: number;
  stockMinimo: number;
  costoUnitario: number;
  precioVenta: number;
}

export interface FiltrosProducto {
  q?: string;
  proveedorId?: number | null;
  categoria?: string | null;
  estado?: EstadoStock | null;
  soloActivos?: boolean;
}

export interface MovimientoRequest {
  productoId: number;
  tipo: TipoMovimiento;
  motivo: MotivoMovimiento;
  cantidad: number;
  valorUnitario?: number | null;
  observacion?: string | null;
}

export interface Movimiento {
  id: number;
  productoId: number;
  productoNombre: string;
  proveedorNombre: string;
  tipo: TipoMovimiento;
  motivo: MotivoMovimiento;
  cantidad: number;
  valorUnitario: number;
  observacion: string | null;
  usuario: string;
  fecha: string;
  existenciaResultante: number | null;
}

/** Página de Spring Data (Page<T>) tal como la serializa el backend. */
export interface Pagina<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FiltrosMovimiento {
  productoId?: number | null;
  proveedorId?: number | null;
  tipo?: TipoMovimiento | null;
  motivo?: MotivoMovimiento | null;
  desde?: string | null;
  hasta?: string | null;
}

export interface Dashboard {
  productosActivos: number;
  productosBajoStock: number;
  productosAgotados: number;
  movimientosHoy: number;
  valorInventario: number;
  ventasDelMes: number;
}

export interface RotacionItem {
  productoId: number;
  productoNombre: string;
  proveedorNombre: string;
  unidadesVendidas: number;
  ingresos: number;
  indiceRotacion: number | null;
  posicion: number;
}

export interface ParticipacionFila {
  proveedorId: number;
  proveedorNombre: string;
  valorInventario: number;
  participacionPct: number;
}

/** Respuesta de GET /api/reportes/participacion (incluye el índice HHI). */
export interface Participacion {
  valorTotalInventario: number;
  indiceHhi: number;
  nivelConcentracion: 'BAJA' | 'MODERADA' | 'ALTA';
  proveedores: ParticipacionFila[];
}
