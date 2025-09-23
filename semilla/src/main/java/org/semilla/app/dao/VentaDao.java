package org.semilla.app.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class VentaDao {
    private final Connection cn;
    public VentaDao(Connection cn) { this.cn = cn; }

    /** Item a persistir (snapshot). */
    public static class VentaItem {
        public int    productoId;
        public String nombreProd;
        public String unidadBase;   // 'un' o 'g'
        public double pvUnitario;   // por unidad o por 100g
        public double cantidad;     // unidades o gramos
        public double subtotal;

        public VentaItem(int productoId, String nombreProd, String unidadBase,
                         double pvUnitario, double cantidad, double subtotal) {
            this.productoId = productoId;
            this.nombreProd = nombreProd;
            this.unidadBase = unidadBase;
            this.pvUnitario = pvUnitario;
            this.cantidad   = cantidad;
            this.subtotal   = subtotal;
        }
    }

    /** Guarda venta + items en transacción. Devuelve id de la venta. */
    public int saveSale(double total, String observacion, List<VentaItem> items) throws SQLException {
        boolean oldAuto = cn.getAutoCommit();
        cn.setAutoCommit(false);
        try {
            // --- ventas: aseguramos columna 'notas' y usamos ese insert ---
            ensureNotasColumnVentas();

            int ventaId;
            String sqlVenta = "INSERT INTO ventas(fecha_hora,total,notas) VALUES (?,?,?)";
            System.out.println("SQL venta (final): " + sqlVenta);

            try (PreparedStatement ps = cn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, LocalDateTime.now().toString()); // ISO-8601
                ps.setDouble(2, total);
                if (observacion != null && !observacion.isBlank()) ps.setString(3, observacion);
                else ps.setNull(3, Types.VARCHAR);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    ventaId = keys.next() ? keys.getInt(1) : -1;
                }
            }

            // --- venta_items: si existe 'precio_unit' (NOT NULL en tu BD), lo seteamos = pv_unitario ---
            boolean hasPrecioUnit = hasColumn("venta_items", "precio_unit");

            String sqlItems = hasPrecioUnit
                    ? "INSERT INTO venta_items(venta_id,producto_id,nombre_prod,unidad_base,pv_unitario,precio_unit,cantidad,subtotal) VALUES (?,?,?,?,?,?,?,?)"
                    : "INSERT INTO venta_items(venta_id,producto_id,nombre_prod,unidad_base,pv_unitario,cantidad,subtotal) VALUES (?,?,?,?,?,?,?)";
            System.out.println("SQL items (final): " + sqlItems);

            try (PreparedStatement it = cn.prepareStatement(sqlItems)) {
                for (VentaItem v : items) {
                    int i = 1;
                    it.setInt(i++, ventaId);
                    it.setInt(i++, v.productoId);
                    it.setString(i++, v.nombreProd);
                    it.setString(i++, v.unidadBase);
                    it.setDouble(i++, v.pvUnitario);
                    if (hasPrecioUnit) it.setDouble(i++, v.pvUnitario); // precio_unit = pv_unitario
                    it.setDouble(i++, v.cantidad);
                    it.setDouble(i++, v.subtotal);
                    it.addBatch();
                }
                it.executeBatch();
            }

            cn.commit();
            return ventaId;
        } catch (SQLException ex) {
            cn.rollback();
            throw ex;
        } finally {
            cn.setAutoCommit(oldAuto);
        }
    }

    // ---------- helpers de esquema ----------
    private void ensureNotasColumnVentas() throws SQLException {
        if (!hasColumn("ventas", "observacion") && !hasColumn("ventas", "notas")) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE ventas ADD COLUMN notas TEXT");
            }
        }
    }

    private boolean hasColumn(String table, String column) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("name"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
