package org.semilla.app.dao;

import org.semilla.app.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    private final Connection cn;
    public CategoryDao(Connection cn) { this.cn = cn; }

    public Category findById(int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,margen_pct_default FROM categorias WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = new Category();
                    c.id = rs.getInt("id");
                    c.nombre = rs.getString("nombre");
                    c.margenPctDefault = rs.getDouble("margen_pct_default");
                    return c;
                }
                return null;
            }
        }
    }

    public List<Category> findAll() throws SQLException {
        List<Category> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,margen_pct_default FROM categorias WHERE activo=1 ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category c = new Category();
                    c.id = rs.getInt("id");
                    c.nombre = rs.getString("nombre");
                    c.margenPctDefault = rs.getDouble("margen_pct_default");
                    out.add(c);
                }
            }
        }
        return out;
    }

    // --- helpers para asegurar categorías por nombre (case-insensitive) ---

    public Integer findIdByNameCI(String nombre) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id FROM categorias WHERE lower(nombre)=lower(?)")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
                return null;
            }
        }
    }

    public int insert(String nombre, double margenPctDefault) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO categorias(nombre,margen_pct_default,activo) VALUES (?,?,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setDouble(2, margenPctDefault);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public void updateMarginAndEnable(int id, double margenPctDefault) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE categorias SET margen_pct_default=?, activo=1 WHERE id=?")) {
            ps.setDouble(1, margenPctDefault);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Crea si no existe, y si existe actualiza el margen (y activa). Devuelve el id. */
    public int ensure(String nombre, double margenPctDefault) throws SQLException {
        Integer id = findIdByNameCI(nombre);
        if (id == null) {
            return insert(nombre, margenPctDefault);
        } else {
            updateMarginAndEnable(id, margenPctDefault);
            return id;
        }
    }
}
