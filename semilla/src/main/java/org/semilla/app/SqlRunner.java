package org.semilla.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SqlRunner {

    public static void runSchema(Connection cn, String resourcePath) throws Exception {
        String sql = readResource(resourcePath);
        try (Statement st = cn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static String readResource(String resourcePath) throws Exception {
        try (var in = SqlRunner.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalStateException("No se encontró recurso: " + resourcePath);
            try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
