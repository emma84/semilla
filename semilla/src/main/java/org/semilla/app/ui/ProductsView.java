package org.semilla.app.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.semilla.app.dao.CategoryDao;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.model.Category;
import org.semilla.app.model.Product;
import org.semilla.app.model.Provider;
import org.semilla.app.service.PriceService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class ProductsView {

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final ProviderDao providerDao;
    private final PriceService priceService;

    private final TableView<ProductRow> table = new TableView<>();
    private final ObservableList<ProductRow> data = FXCollections.observableArrayList();
    private List<Category> categories;

    // Form
    private final TextField tfId = disabled(new TextField());
    private final TextField tfNombre = new TextField();
    private final ComboBox<String> cbTipo = new ComboBox<>();
    private final ComboBox<String> cbUnidad = new ComboBox<>();
    private final TextField tfCosto = new TextField();
    private final TextField tfMargen = new TextField();
    private final ComboBox<Category> cbCategoria = new ComboBox<>();
    private final Label lblPV = new Label("-");
    private final TextField tfCodigoBarra = new TextField();
    private final ComboBox<Provider> cbProveedor = new ComboBox<>();

    public ProductsView(ProductDao productDao, CategoryDao categoryDao, ProviderDao providerDao, PriceService priceService) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.providerDao = providerDao;
        this.priceService = priceService;
    }

    public void show(Stage stage, String title) throws Exception {
        stage.setTitle(title);

        // --- Tabla ---
        var colId     = new TableColumn<ProductRow, Integer>("ID");
        var colNombre = new TableColumn<ProductRow, String>("Nombre");
        var colTipo   = new TableColumn<ProductRow, String>("Tipo");
        var colUnidad = new TableColumn<ProductRow, String>("Unidad");
        var colCodigo = new TableColumn<ProductRow, String>("Código");
        var colCosto  = new TableColumn<ProductRow, Double>("Costo");
        var colMargen = new TableColumn<ProductRow, Double>("Margen %");
        var colPV     = new TableColumn<ProductRow, Double>("PV");
        var colCat    = new TableColumn<ProductRow, Integer>("CatID");
        var colProv   = new TableColumn<ProductRow, String>("Proveedor");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadBase"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarra"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("precioCosto"));
        colMargen.setCellValueFactory(new PropertyValueFactory<>("margenPct"));
        colPV.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoriaId"));
        colProv.setCellValueFactory(new PropertyValueFactory<>("proveedorNombre"));

        colId.setPrefWidth(50);
        colNombre.setPrefWidth(200);
        colTipo.setPrefWidth(80);
        colUnidad.setPrefWidth(70);
        colCodigo.setPrefWidth(140);
        colCosto.setPrefWidth(80);
        colMargen.setPrefWidth(80);
        colPV.setPrefWidth(90);
        colCat.setPrefWidth(60);
        colProv.setPrefWidth(180);

        table.getColumns().setAll(colId, colNombre, colTipo, colUnidad, colCodigo, colCosto, colMargen, colPV, colCat, colProv);
        table.setItems(data);
        table.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> populateForm(b));

        // --- Formulario ---
        cbTipo.setItems(FXCollections.observableArrayList("GRANEL", "UNIDAD"));
        cbUnidad.setItems(FXCollections.observableArrayList("g", "un"));

        // Enlace Tipo ↔ Unidad
        cbTipo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            if (v.equals("GRANEL"))  cbUnidad.getSelectionModel().select("g");
            if (v.equals("UNIDAD"))  cbUnidad.getSelectionModel().select("un");
        });

        HBox row1 = row("ID:", tfId, "Nombre:", tfNombre);
        HBox row2 = row("Tipo:", cbTipo, "Unidad:", cbUnidad);
        HBox row3 = row("Costo:", tfCosto, "Margen% (0 = cat):", tfMargen);
        HBox row4 = row("Categoría:", cbCategoria, "PV:", lblPV);
        HBox row5 = row("Código:", tfCodigoBarra, "", new Label(""));
        HBox row6 = row("Proveedor:", cbProveedor, "", new Label(""));

        // Buscar por código al presionar Enter (sirve para pistola que manda Enter)
        tfCodigoBarra.setOnAction(e -> onScanBarcode());

        Button btnNuevo     = new Button("Nuevo");
        Button btnGuardar   = new Button("Guardar");
        Button btnRecalc    = new Button("Recalcular PV");
        Button btnRefrescar = new Button("Refrescar");
        Button btnProv      = new Button("Proveedores…");

        btnNuevo.setOnAction(e -> clearForm());
        btnGuardar.setOnAction(e -> onSave());
        btnRecalc.setOnAction(e -> onRecalc());
        btnRefrescar.setOnAction(e -> reload());
        btnProv.setOnAction(e -> {
            try {
                new ProvidersView(providerDao).show(stage);
                loadProviders(); // refrescar lista después de cerrar
            } catch (Exception ex) {
                showError("No se pudo abrir Proveedores: " + ex.getMessage());
            }
        });

        HBox actions = new HBox(10, btnNuevo, btnGuardar, btnRecalc, btnRefrescar, btnProv);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(10, row1, row2, row3, row4, row5, row6, actions);
        form.setPadding(new Insets(10));
        form.setPrefWidth(580);

        // Layout
        SplitPane split = new SplitPane(new VBox(table), form);
        split.setDividerPositions(0.58);

        BorderPane root = new BorderPane(split);
        root.setPadding(new Insets(10));

        // Cargar datos base
        categories = categoryDao.findAll();
        cbCategoria.setItems(FXCollections.observableArrayList(categories));
        cbCategoria.setCellFactory(lv -> new ListCell<Category>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.id + " - " + item.nombre);
            }
        });
        cbCategoria.setButtonCell(new ListCell<Category>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.id + " - " + item.nombre);
            }
        });

        loadProviders();
        reload();

        stage.setScene(new Scene(root, 1200, 640));
        stage.show();

        // foco en Código para “escanear” directo
        tfCodigoBarra.requestFocus();
    }

    private void loadProviders() throws Exception {
        var provs = providerDao.findAllActive();
        cbProveedor.setItems(FXCollections.observableArrayList(provs));
        cbProveedor.setCellFactory(lv -> new ListCell<Provider>() {
            @Override protected void updateItem(Provider item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.nombre);
            }
        });
        cbProveedor.setButtonCell(new ListCell<Provider>() {
            @Override protected void updateItem(Provider item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.nombre);
            }
        });
    }

    private void reload() {
        try {
            // mapa id -> nombre de proveedores (incluye inactivos para mostrar históricos)
            var provById = providerDao.findAll();
            var map = new HashMap<Integer, String>();
            for (var pr : provById) if (pr.id != null) map.put(pr.id, pr.nombre);

            var rows = productDao.listAll().stream().map(ProductRow::from).toList();
            for (var r : rows) r.proveedorNombre = (r.proveedorId != null) ? map.get(r.proveedorId) : null;

            data.setAll(rows);
            if (!data.isEmpty()) table.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            showError("Error al cargar productos: " + ex.getMessage());
        }
    }

    private void populateForm(ProductRow r) {
        if (r == null) { clearForm(); return; }
        tfId.setText(s(r.id));
        tfNombre.setText(r.nombre);
        cbTipo.getSelectionModel().select(r.tipo);
        cbUnidad.getSelectionModel().select(r.unidadBase);
        tfCosto.setText(s(r.precioCosto));
        tfMargen.setText(s(r.margenPct));
        lblPV.setText(s(r.precioVenta));
        tfCodigoBarra.setText(s(r.codigoBarra));

        // categoría
        Category matchCat = categories.stream().filter(c -> eq(c.id, r.categoriaId)).findFirst().orElse(null);
        cbCategoria.getSelectionModel().select(matchCat);

        // proveedor
        Provider matchProv = cbProveedor.getItems().stream().filter(pr -> eq(pr.id, r.proveedorId)).findFirst().orElse(null);
        cbProveedor.getSelectionModel().select(matchProv);
    }

    private void clearForm() {
        tfId.clear();
        tfNombre.clear();
        cbTipo.getSelectionModel().clearSelection();
        cbUnidad.getSelectionModel().clearSelection();
        tfCosto.clear();
        tfMargen.setText("0");
        cbCategoria.getSelectionModel().clearSelection();
        lblPV.setText("-");
        tfCodigoBarra.clear();
        cbProveedor.getSelectionModel().clearSelection();
    }

    private void onSave() {
        try {
            Product p = new Product();
            String idText = tfId.getText().trim();
            p.id = idText.isEmpty() ? null : Integer.parseInt(idText);

            p.nombre = tfNombre.getText().trim();
            p.tipo = valueOrNull(cbTipo);
            p.unidadBase = valueOrNull(cbUnidad);
            p.precioCosto = toDouble(tfCosto.getText());
            p.margenPct = toDouble(tfMargen.getText());

            String code = tfCodigoBarra.getText() != null ? tfCodigoBarra.getText().trim() : "";
            p.codigoBarra = code.isEmpty() ? null : code;

            Category selCat = cbCategoria.getSelectionModel().getSelectedItem();
            p.categoriaId = selCat != null ? selCat.id : null;

            Provider selProv = cbProveedor.getSelectionModel().getSelectedItem();
            p.proveedorId = selProv != null ? selProv.id : null;

            if (p.nombre.isEmpty() || p.tipo == null || p.unidadBase == null) {
                showInfo("Completá: Nombre, Tipo y Unidad.");
                return;
            }
            if ("GRANEL".equals(p.tipo) && !"g".equals(p.unidadBase)) { showInfo("Para GRANEL la unidad debe ser 'g'."); return; }
            if ("UNIDAD".equals(p.tipo) && !"un".equals(p.unidadBase)) { showInfo("Para UNIDAD la unidad debe ser 'un'."); return; }

            // Validar duplicados por nombre y por código
            Integer existingByName = productDao.findIdByName(p.nombre);      // case-insensitive
            Integer existingByCode = productDao.findIdByBarcode(p.codigoBarra);

            if (p.id == null) {
                if (existingByName != null) { showInfo("Ya existe un producto con ese nombre."); return; }
                if (existingByCode != null) { showInfo("Ese código de barras ya está asignado a otro producto."); return; }
                p.precioVenta = 0.0;
                int newId = productDao.insert(p);
                priceService.recalcAndPersist(newId);
                reload();
                selectById(newId);
                showInfo("Producto creado (ID=" + newId + ") y PV recalculado.");
            } else {
                if (existingByName != null && !existingByName.equals(p.id)) { showInfo("Ya existe otro producto con ese nombre."); return; }
                if (existingByCode != null && !existingByCode.equals(p.id)) { showInfo("Ese código de barras pertenece a otro producto."); return; }
                productDao.updateBasic(p);
                priceService.recalcAndPersist(p.id);
                reload();
                selectById(p.id);
                showInfo("Producto actualizado y PV recalculado.");
            }
        } catch (SQLException ex) {
            showError("SQL: " + ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onRecalc() {
        var sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Elegí un producto."); return; }
        try {
            priceService.recalcAndPersist(sel.id);
            reload();
            selectById(sel.id);
            showInfo("PV recalculado para ID=" + sel.id);
        } catch (Exception ex) {
            showError("No se pudo recalcular: " + ex.getMessage());
        }
    }

    // Buscar por código cuando la pistola/teclado manda Enter
    private void onScanBarcode() {
        String code = tfCodigoBarra.getText() != null ? tfCodigoBarra.getText().trim() : "";
        if (code.isEmpty()) return;
        try {
            Integer id = productDao.findIdByBarcode(code);
            if (id != null) {
                reload();
                selectById(id);
            } else {
                showInfo("Código no encontrado. Si guardás, quedará asignado al producto nuevo.");
            }
        } catch (Exception ex) {
            showError("Error buscando por código: " + ex.getMessage());
        }
    }

    // ---- helpers ----
    private static TextField disabled(TextField tf) {
        tf.setEditable(false);
        tf.setDisable(true);
        return tf;
    }
    private static HBox row(String l1, Control c1, String l2, Control c2) {
        Label a = new Label(l1); a.setMinWidth(140);
        Label b = new Label(l2); b.setMinWidth(140);
        HBox hb = new HBox(10, a, c1, b, c2);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }
    private static String valueOrNull(ComboBox<String> cb) {
        var v = cb.getSelectionModel().getSelectedItem();
        return v != null && !v.isBlank() ? v : null;
    }
    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static boolean eq(Integer a, Integer b) { return (a == null && b == null) || (a != null && a.equals(b)); }
    private static double toDouble(String s) {
        try { return Double.parseDouble(s.replace(',', '.').trim()); }
        catch (Exception e) { return 0.0; }
    }
    private static void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private static void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }

    private void selectById(int id) {
        for (int i = 0; i < data.size(); i++) {
            Integer rowId = data.get(i).id;
            if (rowId != null && rowId == id) {
                table.getSelectionModel().select(i);
                table.scrollTo(i);
                break;
            }
        }
    }

    // Row para TableView (con GETTERS, incluye código y proveedor)
    public static class ProductRow {
        public Integer id;
        public String  nombre;
        public String  tipo;
        public String  unidadBase;
        public String  codigoBarra;
        public Double  precioCosto;
        public Double  margenPct;
        public Double  precioVenta;
        public Integer categoriaId;
        public Integer proveedorId;
        public String  proveedorNombre;

        public static ProductRow from(Product p) {
            ProductRow r = new ProductRow();
            r.id = p.id; r.nombre = p.nombre; r.tipo = p.tipo; r.unidadBase = p.unidadBase;
            r.codigoBarra = p.codigoBarra;
            r.precioCosto = p.precioCosto; r.margenPct = p.margenPct; r.precioVenta = p.precioVenta;
            r.categoriaId = p.categoriaId; r.proveedorId = p.proveedorId;
            return r;
        }

        public Integer getId() { return id; }
        public String  getNombre() { return nombre; }
        public String  getTipo() { return tipo; }
        public String  getUnidadBase() { return unidadBase; }
        public String  getCodigoBarra() { return codigoBarra; }
        public Double  getPrecioCosto() { return precioCosto; }
        public Double  getMargenPct() { return margenPct; }
        public Double  getPrecioVenta() { return precioVenta; }
        public Integer getCategoriaId() { return categoriaId; }
        public Integer getProveedorId() { return proveedorId; }
        public String  getProveedorNombre() { return proveedorNombre; }
    }
}
