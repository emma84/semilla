// src/main/java/org/semilla/app/Migration.java
package org.semilla.app;

import java.sql.*;

public class Migration {

    private static boolean columnExists(Connection cn, String table, String column) throws SQLException {
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
            return false;
        }
    }

    public static void ensureProveedorColumn(Connection cn) throws SQLException {
        if (!columnExists(cn, "productos", "proveedor_id")) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE productos ADD COLUMN proveedor_id INTEGER REFERENCES proveedores(id)");
            }
        }
    }
}
