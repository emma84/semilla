package org.semilla.app;

import java.sql.*;

public class Migration {

    private static boolean columnExists(Connection cn, String table, String column) throws SQLException {
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            return false;
        }
    }

    private static void ensureColumn(Connection cn, String table, String column, String type) throws SQLException {
        if (!columnExists(cn, table, column)) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        }
    }

    /** De versiones previas: agrega productos.proveedor_id si falta. */
    public static void ensureProveedorColumn(Connection cn) throws SQLException {
        if (!columnExists(cn, "productos", "proveedor_id")) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE productos ADD COLUMN proveedor_id INTEGER REFERENCES proveedores(id)");
            }
        }
    }

    /** NUEVO: agrega productos.stock_actual (REAL NOT NULL DEFAULT 0). */
    public static void ensureStockColumn(Connection cn) throws SQLException {
        if (!columnExists(cn, "productos", "stock_actual")) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE productos ADD COLUMN stock_actual REAL NOT NULL DEFAULT 0");
            }
        }
    }

    /** Índices útiles. */
    public static void ensureIndexes(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_productos_nombre_ci ON productos(lower(nombre));");
            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_productos_codbar_unq ON productos(codigo_barra) " +
                    "WHERE codigo_barra IS NOT NULL AND codigo_barra <> '';");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_venta_items_venta ON venta_items(venta_id);");
        }
    }

    /** Esquema para ventas e items, y asegura columnas que usa el POS. */
    public static void ensureVentasSchema(Connection cn) throws SQLException {
        // Crear tablas base si no existen
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ventas (id INTEGER PRIMARY KEY AUTOINCREMENT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS venta_items (id INTEGER PRIMARY KEY AUTOINCREMENT, venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE)");
        }
        // ventas: columnas requeridas
        ensureColumn(cn, "ventas", "fecha_hora", "TEXT");
        ensureColumn(cn, "ventas", "total", "REAL");
        if (!columnExists(cn, "ventas", "observacion") && !columnExists(cn, "ventas", "notas")) {
            ensureColumn(cn, "ventas", "notas", "TEXT");
        }
        // venta_items: columnas requeridas por el POS
        ensureColumn(cn, "venta_items", "producto_id", "INTEGER");
        ensureColumn(cn, "venta_items", "nombre_prod", "TEXT");
        ensureColumn(cn, "venta_items", "unidad_base", "TEXT");
        ensureColumn(cn, "venta_items", "pv_unitario", "REAL");
        // tu esquema viejo trae precio_unit NOT NULL → lo respetamos
        if (!columnExists(cn, "venta_items", "precio_unit")) {
            // no lo creamos NOT NULL por compatibilidad; si ya existe y es NOT NULL, se mantiene
            ensureColumn(cn, "venta_items", "precio_unit", "REAL");
        }
        ensureColumn(cn, "venta_items", "cantidad", "REAL");
        ensureColumn(cn, "venta_items", "subtotal", "REAL");

        ensureIndexes(cn);
    }
}
