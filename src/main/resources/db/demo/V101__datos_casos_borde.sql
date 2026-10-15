-- ============================================================
-- Datos de casos de borde para demostracion y pruebas manuales
-- ============================================================
--
-- V100 genera 90 dias de historia realista, pero deja todos los
-- productos EN_STOCK, todos los proveedores activos y sin mermas.
-- Con esos datos no se pueden capturar las evidencias de RN-06,
-- RN-07, RN-08, RN-10 (BAJO_STOCK y AGOTADO) ni la tasa de merma.
--
-- Esta migracion agrega exactamente los casos que faltan.
-- Solo se carga en el perfil dev (classpath:db/demo).
--
-- Importante: las entradas se insertan antes que las salidas,
-- porque el trigger trg_validar_salida valida cada fila contra la
-- existencia acumulada hasta ese momento.

-- ------------------------------------------------------------
-- 1. PROVEEDORES
-- ------------------------------------------------------------

INSERT INTO proveedores (nombre, nit, telefono, email, direccion, tiempo_entrega_dias, activo) VALUES
-- Proveedor normal, surte los productos de borde de abajo
('Panaderia El Trigal',  '900200001-1', '3002220001', 'pedidos@trigal.example',  'Carrera 10 # 20-30', 1, TRUE),
-- Proveedor ACTIVO y SIN productos: sirve para demostrar que la
-- inactivacion si funciona cuando no hay nada que lo impida (RN-07)
('Distribuidora Nueva',  '900200002-2', '3002220002', 'contacto@nueva.example',  'Calle 45 # 12-08',   3, TRUE),
-- Proveedor INACTIVO: al intentar crear un producto con el, el
-- sistema debe responder 409 (RN-06)
('Surtidora Retirada',   '900200003-3', '3002220003', 'info@retirada.example',   'Avenida 68 # 5-15',  5, FALSE);

-- ------------------------------------------------------------
-- 2. PRODUCTOS DE BORDE
-- ------------------------------------------------------------

INSERT INTO productos (sku, nombre, categoria, proveedor_id, stock_minimo, costo_unitario, precio_venta, activo) VALUES

-- BAJO_STOCK: terminara con existencia 8 frente a un minimo de 10
('PAN-001', 'Pan tajado 500 g', 'Panaderia',
 (SELECT id FROM proveedores WHERE nit = '900200001-1'), 10, 2800, 3800, TRUE),

-- AGOTADO: terminara con existencia 0
('CAF-001', 'Cafe molido 250 g', 'Abarrotes',
 (SELECT id FROM proveedores WHERE nit = '900200001-1'),  5, 7200, 9500, TRUE),

-- DESHABILITADO con remanente: no admite compras ni ventas, pero si
-- bajas por averia o vencimiento (RN-08)
('VEL-001', 'Velas aromaticas', 'Hogar',
 (SELECT id FROM proveedores WHERE nit = '900200001-1'),  5, 4500, 6000, FALSE),

-- Producto deshabilitado del proveedor inactivo. Se deja inactivo
-- para que el estado del proveedor sea coherente con RN-07.
('SAL-001', 'Sal refinada 1 kg', 'Abarrotes',
 (SELECT id FROM proveedores WHERE nit = '900200003-3'),  8, 1200, 1800, FALSE);

-- ------------------------------------------------------------
-- 3. ENTRADAS (siempre antes de las salidas)
-- ------------------------------------------------------------

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'ENTRADA', 'COMPRA', e.cantidad, p.costo_unitario, e.nota,
       (SELECT id FROM usuarios WHERE username = 'tendero'),
       now() - make_interval(days => e.dias)
FROM (VALUES
    ('PAN-001', 50, 'Compra inicial de panaderia', 30),
    ('CAF-001', 30, 'Compra inicial de cafe',      30),
    ('VEL-001', 20, 'Ultima compra antes de descontinuar', 45),
    ('SAL-001', 15, 'Compra antes de retirar al proveedor', 60)
) AS e(sku, cantidad, nota, dias)
JOIN productos p ON p.sku = e.sku;

-- ------------------------------------------------------------
-- 4. SALIDAS
-- ------------------------------------------------------------

-- PAN-001: 50 entradas - 40 ventas - 2 averias = 8  -> BAJO_STOCK (minimo 10)
INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENTA', 40, p.precio_venta, 'Ventas acumuladas del mes',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '5 days'
FROM productos p WHERE p.sku = 'PAN-001';

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'AVERIA', 2, p.costo_unitario, 'Bolsas rotas en el transporte',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '3 days'
FROM productos p WHERE p.sku = 'PAN-001';

-- CAF-001: 30 entradas - 28 ventas - 2 vencimientos = 0  -> AGOTADO
INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENTA', 28, p.precio_venta, 'Ventas acumuladas',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '7 days'
FROM productos p WHERE p.sku = 'CAF-001';

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENCIMIENTO', 2, p.costo_unitario, 'Lote vencido dado de baja',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '2 days'
FROM productos p WHERE p.sku = 'CAF-001';

-- SAL-001: 15 entradas - 15 ventas = 0, producto ya retirado
INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENTA', 15, p.precio_venta, 'Liquidacion antes de retirar el producto',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '40 days'
FROM productos p WHERE p.sku = 'SAL-001';

-- VEL-001 conserva sus 20 unidades: es el remanente que permite
-- demostrar RN-08 (una baja por averia si se acepta en un producto
-- deshabilitado, una venta no).

-- ------------------------------------------------------------
-- 5. MERMA EN PRODUCTOS DE V100
-- ------------------------------------------------------------
-- Sin estas filas la tasa de merma seria 0 en todo el catalogo y el
-- reporte correspondiente saldria vacio.

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENCIMIENTO', 4, p.costo_unitario, 'Lote proximo a vencer retirado',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '6 days'
FROM productos p WHERE p.sku = 'LEC-001';

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'AVERIA', 3, p.costo_unitario, 'Envases golpeados en bodega',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '4 days'
FROM productos p WHERE p.sku = 'GAS-001';

INSERT INTO movimientos (producto_id, tipo, motivo, cantidad, valor_unitario, observacion, usuario_id, fecha)
SELECT p.id, 'SALIDA', 'VENCIMIENTO', 2, p.costo_unitario, 'Producto vencido en gondola',
       (SELECT id FROM usuarios WHERE username = 'tendero'), now() - interval '8 days'
FROM productos p WHERE p.sku = 'YOG-001';
