package org.semilla.app.dao;

import org.semilla.app.model.Provider;
import javafx.util.StringConverter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProviderDao {
    private final Connection cn;
    public ProviderDao(Connection cn) { this.cn = cn; }

    public List<Provider> findAll() throws SQLException {
        List<Provider> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,cuit,telefono,email,direccion,activo FROM proveedores ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<Provider> findAllActive() throws SQLException {
        List<Provider> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id,nombre,cuit,telefono,email,direccion,activo FROM proveedores WHERE activo=1 ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    // --- Aliases por compatibilidad con vistas que esperan estos nombres ---
    public List<Provider> listAll() throws SQLException { return findAll(); }
    public List<Provider> listAllActive() throws SQLException { return findAllActive(); }

    public Integer findIdByNameCI(String nombre) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id FROM proveedores WHERE lower(nombre)=lower(?)")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("id") : null; }
        }
    }

    public int insert(Provider p) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO proveedores(nombre,cuit,telefono,email,direccion,activo) VALUES (?,?,?,?,?,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.cuit);
            ps.setString(3, p.telefono);
            ps.setString(4, p.email);
            ps.setString(5, p.direccion);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : -1; }
        }
    }

    public void update(Provider p) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE proveedores SET nombre=?, cuit=?, telefono=?, email=?, direccion=?, activo=? WHERE id=?")) {
            ps.setString(1, p.nombre);
            ps.setString(2, p.cuit);
            ps.setString(3, p.telefono);
            ps.setString(4, p.email);
            ps.setString(5, p.direccion);
            ps.setInt(6, p.activo != null ? p.activo : 1);
            ps.setInt(7, p.id);
            ps.executeUpdate();
        }
    }

    public static StringConverter<Provider> providerStringConverter() {
        return new StringConverter<Provider>() {
            @Override public String toString(Provider p) {
                return p == null ? "" : (p.nombre == null || p.nombre.isBlank() ? ("#" + p.id) : p.nombre);
            }
            @Override public Provider fromString(String s) { return null; }
        };
    }

    private static Provider map(ResultSet rs) throws SQLException {
        Provider p = new Provider();
        p.id        = rs.getInt("id");
        p.nombre    = rs.getString("nombre");
        p.cuit      = rs.getString("cuit");
        p.telefono  = rs.getString("telefono");
        p.email     = rs.getString("email");
        p.direccion = rs.getString("direccion");
        p.activo    = rs.getInt("activo");
        return p;
    }
}
