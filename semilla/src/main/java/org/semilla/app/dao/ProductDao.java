package org.semilla.app.dao;

import org.semilla.app.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private final Connection cn;
    public ProductDao(Connection cn) { this.cn = cn; }

    public int insert(Product p) throws SQLException {
        String sql = "INSERT INTO productos(nombre,tipo,unidad_base,codigo_barra,precio_costo,margen_pct,precio_venta,categoria_id,proveedor_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.tipo);
            ps.setString(3, p.unidadBase);
            if (p.codigoBarra != null && !p.codigoBarra.isBlank()) ps.setString(4, p.codigoBarra); else ps.setNull(4, Types.VARCHAR);
            ps.setDouble(5, p.precioCosto != null ? p.precioCosto : 0d);
            ps.setDouble(6, p.margenPct   != null ? p.margenPct   : 0d);
            ps.setDouble(7, p.precioVenta != null ? p.precioVenta : 0d);
            if (p.categoriaId != null) ps.setInt(8, p.categoriaId); else ps.setNull(8, Types.INTEGER);
            if (p.proveedorId != null) ps.setInt(9, p.proveedorId); else ps.setNull(9, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : -1; }
        }
    }

    public void updateBasic(Product p) throws SQLException {
        String sql = "UPDATE productos SET nombre=?, tipo=?, unidad_base=?, codigo_barra=?, precio_costo=?, margen_pct=?, categoria_id=?, proveedor_id=? WHERE id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.tipo);
            ps.setString(3, p.unidadBase);
            if (p.codigoBarra != null && !p.codigoBarra.isBlank()) ps.setString(4, p.codigoBarra); else ps.setNull(4, Types.VARCHAR);
            ps.setDouble(5, p.precioCosto != null ? p.precioCosto : 0d);
            ps.setDouble(6, p.margenPct   != null ? p.margenPct   : 0d);
            if (p.categoriaId != null) ps.setInt(7, p.categoriaId); else ps.setNull(7, Types.INTEGER);
            if (p.proveedorId != null) ps.setInt(8, p.proveedorId); else ps.setNull(8, Types.INTEGER);
            ps.setInt(9, p.id);
            ps.executeUpdate();
        }
    }

    public void updatePrecioVenta(int productId, double precioVenta) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("UPDATE productos SET precio_venta=? WHERE id=?")) {
            ps.setDouble(1, precioVenta);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public Product findById(int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,tipo,unidad_base,codigo_barra,precio_costo,margen_pct,precio_venta,categoria_id,proveedor_id FROM productos WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public Integer findIdByName(String nombre) throws SQLException {
        String sql = "SELECT id FROM productos WHERE lower(nombre) = lower(?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("id") : null; }
        }
    }

    public Integer findIdByBarcode(String code) throws SQLException {
        if (code == null || code.trim().isEmpty()) return null;
        try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM productos WHERE codigo_barra = ?")) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("id") : null; }
        }
    }

    public List<Product> listAll() throws SQLException {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,tipo,unidad_base,codigo_barra,precio_costo,margen_pct,precio_venta,categoria_id,proveedor_id FROM productos ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private static Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.id          = rs.getInt("id");
        p.nombre      = rs.getString("nombre");
        p.tipo        = rs.getString("tipo");
        p.unidadBase  = rs.getString("unidad_base");
        p.codigoBarra = rs.getString("codigo_barra");
        p.precioCosto = rs.getDouble("precio_costo");
        p.margenPct   = rs.getDouble("margen_pct");
        p.precioVenta = rs.getDouble("precio_venta");
        int catId     = rs.getInt("categoria_id");
        p.categoriaId = rs.wasNull() ? null : catId;
        int provId    = rs.getInt("proveedor_id");
        p.proveedorId = rs.wasNull() ? null : provId;
        return p;
    }
}
