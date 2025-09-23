package org.semilla.app.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.semilla.app.dao.ProductDao;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.model.Product;
import org.semilla.app.model.Provider;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductsView {

    private final ProductDao productDao;
    private final ProviderDao providerDao;

    private final TableView<Product> table = new TableView<>();
    private final ObservableList<Product> data = FXCollections.observableArrayList();

    // cache proveedores (id -> Provider)
    private final Map<Integer, Provider> providerById = new HashMap<>();

    // Formulario
    private final TextField tfId         = new TextField();
    private final TextField tfNombre     = new TextField();
    private final ComboBox<String>  cbTipo   = new ComboBox<>();
    private final ComboBox<String>  cbUnidad = new ComboBox<>();
    private final TextField tfCosto      = new TextField();
    private final TextField tfMargen     = new TextField();
    private final TextField tfPv         = new TextField();
    private final TextField tfCodBar     = new TextField();
    private final ComboBox<Provider> cbProv = new ComboBox<Provider>();
    private final TextField tfStock      = new TextField();

    public ProductsView(ProductDao productDao, ProviderDao providerDao) {
        this.productDao = productDao;
        this.providerDao = providerDao;
    }

    public void show(Stage stage) {
        stage.setTitle("Productos");

        // ==== Tabla ====
        TableColumn<Product, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id));
        colId.setPrefWidth(70);

        TableColumn<Product, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().nombre != null ? c.getValue().nombre : ""));
        colNombre.setPrefWidth(240);

        TableColumn<Product, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().tipo != null ? c.getValue().tipo : ""));
        colTipo.setPrefWidth(90);

        TableColumn<Product, String> colUnidad = new TableColumn<>("Unidad");
        colUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().unidadBase != null ? c.getValue().unidadBase : ""));
        colUnidad.setPrefWidth(80);

        TableColumn<Product, Double> colPv = new TableColumn<>("PV");
        colPv.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().precioVenta != null ? c.getValue().precioVenta : 0.0));
        colPv.setPrefWidth(100);

        TableColumn<Product, Double> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().stockActual != null ? c.getValue().stockActual : 0.0));
        colStock.setPrefWidth(100);

        TableColumn<Product, String> colProvNombre = new TableColumn<>("Proveedor");
        colProvNombre.setCellValueFactory(cell -> {
            Integer pid = cell.getValue().proveedorId;
            String name = "";
            if (pid != null) {
                Provider sel = providerById.get(pid);
                if (sel != null) name = Optional.ofNullable(sel.nombre).orElse("");
            }
            return new ReadOnlyStringWrapper(name);
        });
        colProvNombre.setPrefWidth(180);

        TableColumn<Product, String> colCodBar = new TableColumn<>("Código");
        colCodBar.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().codigoBarra != null ? c.getValue().codigoBarra : ""));
        colCodBar.setPrefWidth(130);

        table.getColumns().setAll(colId, colNombre, colTipo, colUnidad, colPv, colStock, colProvNombre, colCodBar);
        table.setItems(data);
        table.setPlaceholder(new Label("Sin productos"));

        // Resaltado de bajo stock + lectura correcta en seleccionado
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            Runnable apply = () -> {
                Product p = row.getItem();
                if (p == null) { row.setStyle(""); return; }
                boolean low = isLowStock(p);
                if (!low) { row.setStyle(""); return; }

                if (row.isSelected()) {
                    // Usar colores de selección por defecto (se lee bien)
                    row.setStyle("-fx-background-color: -fx-selection-bar;");
                } else {
                    // Amarillo suave cuando NO está seleccionado
                    row.setStyle("-fx-background-color: #fff3cd;");
                }
            };
            row.itemProperty().addListener((obs, old, p) -> apply.run());
            row.selectedProperty().addListener((obs, was, is) -> apply.run());
            return row;
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, p) -> loadToForm(p));

        // ==== Form ====
        tfId.setPromptText("ID (editable)");
        tfNombre.setPromptText("Nombre");
        cbTipo.getItems().addAll("UNIDAD", "GRANEL");
        cbUnidad.getItems().addAll("un", "g");
        tfCosto.setPromptText("Costo");
        tfMargen.setPromptText("Margen %");
        tfPv.setPromptText("PV (un / x100g)");
        tfCodBar.setPromptText("Código de barras");
        tfStock.setPromptText("Stock (un / g)");

        cbProv.setConverter(ProviderDao.providerStringConverter());
        cbProv.setPromptText("Proveedor…");

        Button btnNew     = new Button("Nuevo");
        Button btnSave    = new Button("Guardar");
        Button btnDel     = new Button("Eliminar");
        Button btnRef     = new Button("Refrescar");
        Button btnStock0  = new Button("Stock 0 (todos)");
        Button btnStockMas1 = new Button("+1");
        Button btnStockMen1 = new Button("-1");
        Button btnLow     = new Button("Bajo stock"); // NUEVO

        btnNew.setOnAction(e -> clearForm());
        btnRef.setOnAction(e -> reload());
        btnStock0.setOnAction(e -> onStockZeroAll());
        btnStockMas1.setOnAction(e -> bumpStock(+1));
        btnStockMen1.setOnAction(e -> bumpStock(-1));
        btnLow.setOnAction(e -> showLowStock());

        btnSave.setOnAction(e -> onSave());
        btnDel.setOnAction(e -> onDelete());

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(8);
        int r=0;
        form.add(new Label("ID:"), 0, r);   form.add(tfId, 1, r); r++;
        form.add(new Label("Nombre:"), 0, r); form.add(tfNombre, 1, r, 3, 1); r++;
        form.add(new Label("Tipo:"), 0, r); form.add(cbTipo, 1, r);
        form.add(new Label("Unidad:"), 2, r); form.add(cbUnidad, 3, r); r++;
        form.add(new Label("Costo:"), 0, r); form.add(tfCosto, 1, r);
        form.add(new Label("Margen%:"), 2, r); form.add(tfMargen, 3, r); r++;
        form.add(new Label("PV:"), 0, r); form.add(tfPv, 1, r);
        form.add(new Label("Código:"), 2, r); form.add(tfCodBar, 3, r); r++;
        form.add(new Label("Proveedor:"), 0, r); form.add(cbProv, 1, r, 3,1); r++;
        form.add(new Label("Stock:"), 0, r);
        HBox stockRow = new HBox(6, tfStock, btnStockMen1, btnStockMas1);
        form.add(stockRow, 1, r, 3, 1); r++;

        HBox actions = new HBox(10, btnNew, btnSave, btnDel, btnRef, new Separator(), btnStock0, btnLow);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10, table, actions);
        left.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox right = new VBox(10, form);
        right.setPadding(new Insets(10));

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.55);

        Scene scene = new Scene(split, 1200, 650);
        stage.setScene(scene);
        stage.show();

        loadProviders();  // carga proveedores primero (para mostrar nombre en la tabla)
        reload();         // y luego carga productos
    }

    private void loadProviders() {
        try {
            // activos por defecto; si preferís todos, cambialo a findAll()
            var provs = providerDao.findAllActive();
            cbProv.getItems().setAll(provs);
            providerById.clear();
            for (Provider p : provs) providerById.put(p.id, p);
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "No se pudieron cargar proveedores: " + ex.getMessage());
        }
    }

    private void reload() {
        try {
            data.setAll(productDao.listAll());
            if (!data.isEmpty()) table.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "No se pudieron listar productos: " + ex.getMessage());
        }
    }

    private void loadToForm(Product p) {
        if (p == null) { clearForm(); return; }
        tfId.setText(p.id != null ? String.valueOf(p.id) : "");
        tfNombre.setText(p.nombre != null ? p.nombre : "");
        cbTipo.getSelectionModel().select(p.tipo != null ? p.tipo : "UNIDAD");
        cbUnidad.getSelectionModel().select(p.unidadBase != null ? p.unidadBase : "un");
        tfCosto.setText(p.precioCosto != null ? String.valueOf(p.precioCosto) : "");
        tfMargen.setText(p.margenPct != null ? String.valueOf(p.margenPct) : "");
        tfPv.setText(p.precioVenta != null ? String.valueOf(p.precioVenta) : "");
        tfCodBar.setText(p.codigoBarra != null ? p.codigoBarra : "");
        // proveedor ⇄ proveedorId
        if (p.proveedorId != null) {
            Provider sel = providerById.get(p.proveedorId);
            if (sel != null) cbProv.getSelectionModel().select(sel);
            else cbProv.getSelectionModel().clearSelection();
        } else {
            cbProv.getSelectionModel().clearSelection();
        }
        tfStock.setText(p.stockActual != null ? String.valueOf(p.stockActual) : "0");
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        tfId.clear(); tfNombre.clear(); tfCosto.clear(); tfMargen.clear(); tfPv.clear(); tfCodBar.clear(); tfStock.setText("0");
        cbTipo.getSelectionModel().select("UNIDAD");
        cbUnidad.getSelectionModel().select("un");
        cbProv.getSelectionModel().clearSelection();
    }

    private void onSave() {
        try {
            Product p = new Product();
            p.nombre     = safeTrim(tfNombre.getText());
            p.tipo       = cbTipo.getValue();
            p.unidadBase = cbUnidad.getValue();
            p.precioCosto= parseDoubleOrNull(tfCosto.getText());
            p.margenPct  = parseDoubleOrNull(tfMargen.getText());
            p.precioVenta= parseDoubleOrNull(tfPv.getText());
            p.codigoBarra= safeTrimNull(tfCodBar.getText());
            Provider sel = cbProv.getValue();
            p.proveedorId= (sel != null ? sel.id : null);
            p.stockActual= parseDoubleOrZero(tfStock.getText());

            String idTxt = safeTrim(tfId.getText());
            if (idTxt.isEmpty()) {
                int newId = productDao.insert(p);
                p.id = newId;
                data.add(p);
                table.getSelectionModel().select(p);
            } else {
                p.id = Integer.parseInt(idTxt);
                productDao.update(p);
                reload();
                data.stream().filter(x -> x.id != null && x.id.equals(p.id))
                        .findFirst().ifPresent(x -> table.getSelectionModel().select(x));
            }
            alert(Alert.AlertType.INFORMATION, "OK", "Producto guardado.");
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "No se pudo guardar: " + ex.getMessage());
        }
    }

    private void onDelete() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) { alert(Alert.AlertType.INFORMATION, "Atención", "Elegí un producto."); return; }
        alert(Alert.AlertType.INFORMATION, "Atención", "Borrado de productos: aún no implementado.");
    }

    private void onStockZeroAll() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Esto pondrá el stock de TODOS los productos en 0.\n¿Confirmás?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmar");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.YES) return;
        try {
            int rows = productDao.resetAllStockToZero();
            reload();
            alert(Alert.AlertType.INFORMATION, "OK", "Stock reseteado a 0 en " + rows + " productos.");
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "No se pudo resetear stock: " + ex.getMessage());
        }
    }

    private void bumpStock(int step) {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) { alert(Alert.AlertType.INFORMATION, "Atención", "Elegí un producto."); return; }
        try {
            double delta = step; // GRANEL: 1 = 1 gramo
            productDao.addStock(p.id, delta);
            reload();
            data.stream().filter(x -> x.id.equals(p.id)).findFirst().ifPresent(x -> table.getSelectionModel().select(x));
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "No se pudo ajustar stock: " + ex.getMessage());
        }
    }

    // === Bajo stock ===

    private boolean isLowStock(Product p) {
        double s = p.stockActual != null ? p.stockActual : 0.0;
        String u = p.unidadBase != null ? p.unidadBase : "un";
        if ("g".equalsIgnoreCase(u)) {
            return s <= 1000.0; // 1 kg
        } else {
            return s <= 2.0;    // 2 unidades
        }
    }

    private void showLowStock() {
        List<Product> low = data.stream().filter(this::isLowStock).collect(Collectors.toList());
        if (low.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Bajo stock", "No hay productos en bajo stock.");
            return;
        }

        TableView<Product> tv = new TableView<>();
        tv.setItems(FXCollections.observableArrayList(low));

        TableColumn<Product, String> cNombre = new TableColumn<>("Nombre");
        cNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().nombre != null ? c.getValue().nombre : ""));
        cNombre.setPrefWidth(300);

        TableColumn<Product, String> cProv = new TableColumn<>("Proveedor");
        cProv.setCellValueFactory(c -> {
            Integer pid = c.getValue().proveedorId;
            String name = "";
            if (pid != null) {
                Provider sel = providerById.get(pid);
                if (sel != null) name = Optional.ofNullable(sel.nombre).orElse("");
            }
            return new ReadOnlyStringWrapper(name);
        });
        cProv.setPrefWidth(200);

        TableColumn<Product, String> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(c -> {
            double s = c.getValue().stockActual != null ? c.getValue().stockActual : 0.0;
            String u = c.getValue().unidadBase != null ? c.getValue().unidadBase : "un";
            String txt = "g".equalsIgnoreCase(u) ? String.format("%.0f g", s) : String.format("%.0f un", s);
            return new ReadOnlyStringWrapper(txt);
        });
        cCant.setPrefWidth(120);

        tv.getColumns().setAll(cNombre, cCant, cProv);

        Stage s = new Stage();
        s.setTitle("Productos en bajo stock (" + low.size() + ")");
        VBox root = new VBox(10, tv);
        root.setPadding(new Insets(10));
        s.setScene(new Scene(root, 700, 500));
        s.show();
    }

    // === util ===
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return Double.parseDouble(s.replace(',', '.').trim());
    }
    private double parseDoubleOrZero(String s) {
        try { Double v = parseDoubleOrNull(s); return v == null ? 0.0 : v; } catch (Exception e) { return 0.0; }
    }
    private String safeTrim(String s){ return s==null? "": s.trim(); }
    private String safeTrimNull(String s){ return (s==null||s.isBlank()) ? null : s.trim(); }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK); a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
