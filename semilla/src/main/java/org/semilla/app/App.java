package org.semilla.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.*;
import java.sql.Connection;

public class App {

    private static final String DEFAULT_CONFIG_LOCAL = "config.json";
    private static final Path   PROD_CONFIG          = Paths.get("C:/Semilla/config.json");

    public static void main(String[] args) throws Exception {
        Config cfg = loadConfig();
        System.out.println("Config usada: " + cfg);

        // Crear carpetas de datos/backups si faltan
        Files.createDirectories(Paths.get(cfg.dbPath).toAbsolutePath().getParent());
        if (cfg.backupDir != null && !cfg.backupDir.isBlank()) {
            Files.createDirectories(Paths.get(cfg.backupDir));
        }

        Db db = new Db(cfg.dbPath);

        // Abrimos la conexión y mantenemos abierta durante el menú
        try (Connection cn = db.open()) {
            // 1) Schema
            SqlRunner.runSchema(cn, "/schema.sql");

            // 2) DAOs + Servicio de precios (toma la regla desde config)
            var catDao   = new org.semilla.app.dao.CategoryDao(cn);
            var prodDao  = new org.semilla.app.dao.ProductDao(cn);
            var priceSvc = new org.semilla.app.service.PriceService(
                    prodDao, catDao, cfg.roundingRule == null ? "none" : cfg.roundingRule
            );

            // 3) DEMO: insertar una sola vez por nombre
            var p = new org.semilla.app.model.Product();
            p.nombre      = "Chía a granel (demo)";
            p.tipo        = "GRANEL";
            p.unidadBase  = "g";
            p.precioCosto = 150.0;
            p.margenPct   = 80.0; // si 0, toma margen de categoría
            p.categoriaId = 1;    // 'Granel Semillas' del schema
            p.precioVenta = 0.0;

            Integer existingId = prodDao.findIdByName(p.nombre);
            if (existingId == null) {
                int demoId = prodDao.insert(p);
                priceSvc.recalcAndPersist(demoId);
                System.out.println("✔ Producto demo creado con PV recalculado. ID=" + demoId);
            } else {
                System.out.println("↪ Producto demo ya existe (ID=" + existingId + "), no se inserta.");
            }

            // 4) Menú por consola (mantener dentro del try para usar la misma conexión)
            new org.semilla.app.ConsoleMenu(prodDao, catDao, priceSvc).run();
        }

        // Mensaje al salir del menú
        System.out.println("✔ BD lista en: " + cfg.dbPath);
    }

    private static Config loadConfig() throws Exception {
        ObjectMapper om = new ObjectMapper();

        File prod = PROD_CONFIG.toFile();
        if (prod.exists()) {
            return om.readValue(prod, Config.class);
        }
        File local = Paths.get(DEFAULT_CONFIG_LOCAL).toFile();
        if (local.exists()) {
            return om.readValue(local, Config.class);
        }

        // default
        Config c = new Config();
        c.dbPath       = "C:\\Semilla\\data\\semilla.db";
        c.backupDir    = "C:\\Semilla\\backups";
        c.roundingRule = "none"; // podés poner "round_2_dec", "ceil_peso", "ceil_10"
        return c;
    }
}
