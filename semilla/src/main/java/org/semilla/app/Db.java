package org.semilla.app;

import java.nio.file.*;
import java.sql.*;

public class Db {
    private final String jdbcUrl;

    public Db(String dbPath) throws Exception {
        String normalized = dbPath.replace("\\", "/");
        this.jdbcUrl = "jdbc:sqlite:" + normalized;

        Path p = Paths.get(dbPath).toAbsolutePath();
        Files.createDirectories(p.getParent());
    }

    public Connection open() throws SQLException {
        Connection cn = DriverManager.getConnection(jdbcUrl);
        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("PRAGMA foreign_keys=ON;");
        }
        return cn;
    }
}
