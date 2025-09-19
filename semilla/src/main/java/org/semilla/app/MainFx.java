package org.semilla.app;

import javafx.application.Application;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.semilla.app.dao.CategoryDao;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.service.PriceService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

public class MainFx extends Application {

    private static final String DEFAULT_CONFIG_LOCAL = "config.json";
    private static final Path   PROD_CONFIG          = Paths.get("C:/Semilla/config.json");

    private Connection cn;

    @Override
    public void start(Stage stage) throws Exception {
        // 1) Cargar config y asegurar carpetas
        Config cfg = loadConfig();
        Files.createDirectories(Paths.get(cfg.dbPath).toAbsolutePath().getParent());
        if (cfg.backupDir != null && !cfg.backupDir.isBlank()) {
            Files.createDirectories(Paths.get(cfg.backupDir));
        }

        // 2) Abrir DB y aplicar schema/migraciones
        Db db = new Db(cfg.dbPath);
        cn = db.open();
        SqlRunner.runSchema(cn, "/schema.sql");
        // asegurar columna proveedor_id en productos (si actualizaste desde una versión previa)
        org.semilla.app.Migration.ensureProveedorColumn(cn);

        // 3) DAOs y servicios
        var catDao   = new CategoryDao(cn);
        var prodDao  = new ProductDao(cn);
        var provDao  = new ProviderDao(cn);
        var priceSvc = new PriceService(prodDao, catDao, cfg.roundingRule == null ? "none" : cfg.roundingRule);

        // 4) UI principal de Productos (sin pre-cargar proveedores por código)
        var view = new org.semilla.app.ui.ProductsView(prodDao, catDao, provDao, priceSvc);
        view.show(stage, "Semilla · Productos");
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

        // valores por defecto si no hay config.json
        Config c = new Config();
        c.dbPath       = "C:\\Semilla\\data\\semilla.db";
        c.backupDir    = "C:\\Semilla\\backups";
        c.roundingRule = "round_2_dec"; // podés usar "none", "ceil_peso", "ceil_10"
        return c;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
