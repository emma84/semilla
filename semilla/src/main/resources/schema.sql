PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS categorias (
  id                 INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre             TEXT NOT NULL UNIQUE,
  margen_pct_default REAL NOT NULL DEFAULT 0,
  activo             INTEGER NOT NULL DEFAULT 1,
  creado_en          TEXT NOT NULL DEFAULT (datetime('now')),
  actualizado_en     TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS productos (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre         TEXT NOT NULL UNIQUE,
  tipo           TEXT NOT NULL CHECK (tipo IN ('GRANEL','UNIDAD')),
  unidad_base    TEXT NOT NULL CHECK (unidad_base IN ('g','un')),
  codigo_barra   TEXT,
  precio_costo   REAL NOT NULL DEFAULT 0,
  margen_pct     REAL NOT NULL DEFAULT 0,
  precio_venta   REAL NOT NULL DEFAULT 0,
  stock_g        REAL NOT NULL DEFAULT 0,
  stock_un       REAL NOT NULL DEFAULT 0,
  stock_min      REAL NOT NULL DEFAULT 0,
  activo         INTEGER NOT NULL DEFAULT 1,
  categoria_id   INTEGER REFERENCES categorias(id),
  creado_en      TEXT NOT NULL DEFAULT (datetime('now')),
  actualizado_en TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id);

CREATE TABLE IF NOT EXISTS proveedores (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre         TEXT NOT NULL,
  cuit           TEXT,
  telefono       TEXT,
  email          TEXT,
  direccion      TEXT,
  activo         INTEGER NOT NULL DEFAULT 1,
  creado_en      TEXT NOT NULL DEFAULT (datetime('now')),
  actualizado_en TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS compras (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  proveedor_id  INTEGER NOT NULL,
  fecha         TEXT NOT NULL DEFAULT (date('now')),
  medio_pago    TEXT NOT NULL DEFAULT 'efectivo',
  total         REAL NOT NULL DEFAULT 0,
  notas         TEXT,
  creado_en     TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE IF NOT EXISTS compra_items (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  compra_id    INTEGER NOT NULL,
  producto_id  INTEGER NOT NULL,
  cantidad     REAL NOT NULL,
  costo_unit   REAL NOT NULL,
  subtotal     REAL NOT NULL,
  lote         TEXT,
  vencimiento  TEXT,
  FOREIGN KEY (compra_id)  REFERENCES compras(id),
  FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE INDEX IF NOT EXISTS idx_compra_items_compra   ON compra_items(compra_id);
CREATE INDEX IF NOT EXISTS idx_compra_items_producto ON compra_items(producto_id);

CREATE TABLE IF NOT EXISTS ventas (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha           TEXT NOT NULL DEFAULT (datetime('now')),
  medio_pago      TEXT NOT NULL DEFAULT 'efectivo',
  descuento_total REAL NOT NULL DEFAULT 0,
  total           REAL NOT NULL DEFAULT 0,
  notas           TEXT,
  creado_en       TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS venta_items (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  venta_id     INTEGER NOT NULL,
  producto_id  INTEGER NOT NULL,
  cantidad     REAL NOT NULL,
  precio_unit  REAL NOT NULL,
  subtotal     REAL NOT NULL,
  FOREIGN KEY (venta_id)   REFERENCES ventas(id),
  FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE INDEX IF NOT EXISTS idx_venta_items_venta    ON venta_items(venta_id);
CREATE INDEX IF NOT EXISTS idx_venta_items_producto ON venta_items(producto_id);

CREATE TABLE IF NOT EXISTS egresos (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  fecha       TEXT NOT NULL DEFAULT (date('now')),
  categoria   TEXT NOT NULL,
  descripcion TEXT,
  monto       REAL NOT NULL,
  medio_pago  TEXT NOT NULL DEFAULT 'efectivo',
  creado_en   TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TRIGGER IF NOT EXISTS trg_productos_updated
AFTER UPDATE ON productos
BEGIN
  UPDATE productos SET actualizado_en = datetime('now') WHERE id = NEW.id;
END;

INSERT OR IGNORE INTO categorias (id, nombre, margen_pct_default) VALUES
  (1, 'Granel Semillas', 80),
  (2, 'Suplementos', 50),
  (3, 'Frutos Secos', 70);

-- Código de barras único si no es nulo/ vacío
CREATE UNIQUE INDEX IF NOT EXISTS idx_productos_codbar_unq
ON productos(codigo_barra)
WHERE codigo_barra IS NOT NULL AND codigo_barra <> '';

CREATE UNIQUE INDEX IF NOT EXISTS idx_proveedores_nombre_unq ON proveedores(nombre);
CREATE UNIQUE INDEX IF NOT EXISTS idx_productos_codbar_unq
ON productos(codigo_barra)
WHERE codigo_barra IS NOT NULL AND codigo_barra <> '';

