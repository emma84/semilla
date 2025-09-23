package org.semilla.app;

import javafx.application.Application;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.semilla.app.dao.CategoryDao;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.dao.VentaDao;
import org.semilla.app.service.PriceService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class MainFx extends Application {

    private static final String DEFAULT_CONFIG_LOCAL = "config.json";
    private static final Path   PROD_CONFIG          = Paths.get("C:/Semilla/config.json");

    private Connection cn;

    @Override
    public void start(Stage stage) throws Exception {
        // 1) Cargar config + carpetas
        Config cfg = loadConfig();
        System.out.println("Config usada: " + cfg);
        Files.createDirectories(Paths.get(cfg.dbPath).toAbsolutePath().getParent());
        if (cfg.backupDir != null && !cfg.backupDir.isBlank()) {
            Files.createDirectories(Paths.get(cfg.backupDir));
        }

        // 2) DB + schema + migraciones
        Db db = new Db(cfg.dbPath);
        cn = db.open();
        SqlRunner.runSchema(cn, "/schema.sql");
        Migration.ensureProveedorColumn(cn);
        Migration.ensureStockColumn(cn);
        Migration.ensureVentasSchema(cn);   // crea/ajusta ventas y venta_items
        Migration.ensureIndexes(cn);        // índices útiles

        // --- DEBUG: confirmar archivo y columnas que ve la app ---
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA database_list")) {
            while (rs.next()) {
                System.out.println("DB ATTACHED -> name=" + rs.getString(2) + " file=" + rs.getString(3));
            }
        }
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(ventas)")) {
            StringBuilder b = new StringBuilder("ventas cols -> ");
            while (rs.next()) b.append(rs.getString("name")).append(" | ");
            System.out.println(b.toString());
        }
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(venta_items)")) {
            StringBuilder b = new StringBuilder("venta_items cols -> ");
            while (rs.next()) b.append(rs.getString("name")).append(" | ");
            System.out.println(b.toString());
        }
        // ----------------------------------------------------------

        // 3) DAOs y servicios
        var catDao   = new CategoryDao(cn);
        var prodDao  = new ProductDao(cn);
        var provDao  = new ProviderDao(cn); // hoy no lo usamos en POS, queda listo
        var ventaDao = new VentaDao(cn);
        System.out.println("VentaDao loaded from: " +
                org.semilla.app.dao.VentaDao.class.getProtectionDomain().getCodeSource().getLocation());
        var priceSvc = new PriceService(prodDao, catDao, cfg.roundingRule == null ? "none" : cfg.roundingRule);

        // 4) Pantalla principal = POS
        var pos = new org.semilla.app.ui.SalesPosView(prodDao, ventaDao, provDao);

        pos.show(stage, "Semilla · Ventas (POS)");
    }

    @Override
    public void stop() throws Exception {
        if (cn != null && !cn.isClosed()) cn.close();
    }

    private static Config loadConfig() throws Exception {
        ObjectMapper om = new ObjectMapper();
        File prod = PROD_CONFIG.toFile();
        if (prod.exists()) return om.readValue(prod, Config.class);
        File local = Paths.get(DEFAULT_CONFIG_LOCAL).toFile();
        if (local.exists()) return om.readValue(local, Config.class);

        Config c = new Config();
        c.dbPath       = "C:\\Semilla\\data\\semilla.db";
        c.backupDir    = "C:\\Semilla\\backups";
        c.roundingRule = "round_2_dec";
        return c;
    }

    public static void main(String[] args) { launch(args); }
}
