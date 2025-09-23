package org.semilla.app.dao;

import org.semilla.app.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private final Connection cn;
    public ProductDao(Connection cn) { this.cn = cn; }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.id          = rs.getInt("id");
        p.nombre      = rs.getString("nombre");
        p.tipo        = rs.getString("tipo");
        p.unidadBase  = rs.getString("unidad_base");
        p.precioCosto = rs.getObject("precio_costo") != null ? rs.getDouble("precio_costo") : null;
        p.margenPct   = rs.getObject("margen_pct")   != null ? rs.getDouble("margen_pct")   : null;
        p.categoriaId = rs.getObject("categoria_id") != null ? rs.getInt("categoria_id")    : null;
        p.precioVenta = rs.getObject("precio_venta") != null ? rs.getDouble("precio_venta") : null;
        p.codigoBarra = rs.getString("codigo_barra");
        p.proveedorId = rs.getObject("proveedor_id") != null ? rs.getInt("proveedor_id")    : null;
        // stock_actual puede no existir en esquemas muy viejos: lo tratamos con getMeta
        try { p.stockActual = rs.getObject("stock_actual") != null ? rs.getDouble("stock_actual") : 0.0; }
        catch (SQLException ignore) { p.stockActual = 0.0; }
        return p;
    }

    public Product findById(int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT * FROM productos WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public Product findByBarcode(String barcode) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT * FROM productos WHERE codigo_barra=?")) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Product> searchByName(String query, int limit) throws SQLException {
        String q = "%" + query.toLowerCase() + "%";
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT * FROM productos WHERE lower(nombre) LIKE ? ORDER BY nombre LIMIT ?")) {
            ps.setString(1, q); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<Product> listAll() throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT * FROM productos ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public int insert(Product p) throws SQLException {
        String sql = "INSERT INTO productos(nombre,tipo,unidad_base,precio_costo,margen_pct,categoria_id,precio_venta,codigo_barra,proveedor_id,stock_actual) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.tipo);
            ps.setString(3, p.unidadBase);
            if (p.precioCosto != null) ps.setDouble(4, p.precioCosto); else ps.setNull(4, Types.REAL);
            if (p.margenPct   != null) ps.setDouble(5, p.margenPct);   else ps.setNull(5, Types.REAL);
            if (p.categoriaId != null) ps.setInt(6, p.categoriaId);    else ps.setNull(6, Types.INTEGER);
            if (p.precioVenta != null) ps.setDouble(7, p.precioVenta); else ps.setNull(7, Types.REAL);
            if (p.codigoBarra != null && !p.codigoBarra.isBlank()) ps.setString(8, p.codigoBarra); else ps.setNull(8, Types.VARCHAR);
            if (p.proveedorId != null) ps.setInt(9, p.proveedorId);    else ps.setNull(9, Types.INTEGER);
            ps.setDouble(10, p.stockActual != null ? p.stockActual : 0.0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : -1; }
        }
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE productos SET nombre=?,tipo=?,unidad_base=?,precio_costo=?,margen_pct=?,categoria_id=?,precio_venta=?,codigo_barra=?,proveedor_id=?,stock_actual=? WHERE id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.tipo);
            ps.setString(3, p.unidadBase);
            if (p.precioCosto != null) ps.setDouble(4, p.precioCosto); else ps.setNull(4, Types.REAL);
            if (p.margenPct   != null) ps.setDouble(5, p.margenPct);   else ps.setNull(5, Types.REAL);
            if (p.categoriaId != null) ps.setInt(6, p.categoriaId);    else ps.setNull(6, Types.INTEGER);
            if (p.precioVenta != null) ps.setDouble(7, p.precioVenta); else ps.setNull(7, Types.REAL);
            if (p.codigoBarra != null && !p.codigoBarra.isBlank()) ps.setString(8, p.codigoBarra); else ps.setNull(8, Types.VARCHAR);
            if (p.proveedorId != null) ps.setInt(9, p.proveedorId);    else ps.setNull(9, Types.INTEGER);
            ps.setDouble(10, p.stockActual != null ? p.stockActual : 0.0);
            ps.setInt(11, p.id);
            ps.executeUpdate();
        }
    }

    /** Suma delta (positivo o negativo) al stock_actual. Devuelve filas afectadas. */
    public int addStock(int productId, double delta) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("UPDATE productos SET stock_actual = COALESCE(stock_actual,0) + ? WHERE id=?")) {
            ps.setDouble(1, delta);
            ps.setInt(2, productId);
            return ps.executeUpdate();
        }
    }

    /** Setea el stock a un nuevo valor. */
    public int setStock(int productId, double newValue) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("UPDATE productos SET stock_actual=? WHERE id=?")) {
            ps.setDouble(1, newValue);
            ps.setInt(2, productId);
            return ps.executeUpdate();
        }
    }

    /** Pone todo el stock en 0. */
    public int resetAllStockToZero() throws SQLException {
        try (Statement st = cn.createStatement()) {
            return st.executeUpdate("UPDATE productos SET stock_actual = 0");
        }
    }

    // Busca el ID por nombre (case-insensitive). Devuelve null si no existe.
    public Integer findIdByName(String nombre) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id FROM productos WHERE lower(nombre)=lower(?) LIMIT 1")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    // Actualiza solo el precio de venta. Devuelve filas afectadas (0 o 1).
    public void updatePrecioVenta(int productId, double nuevoPv) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE productos SET precio_venta=? WHERE id=?")) {
            ps.setDouble(1, nuevoPv);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
}
