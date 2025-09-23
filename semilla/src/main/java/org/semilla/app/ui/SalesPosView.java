package org.semilla.app.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.dao.VentaDao;
import org.semilla.app.model.Product;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SalesPosView {

    private final ProductDao productDao;
    private final VentaDao   ventaDao;
    private final ProviderDao providerDao; // nuevo: para abrir ProductsView

    // UI
    private final TextField tfSearch = new TextField();
    private final ListView<Product> lvResults = new ListView<>();
    private final TableView<CartItem> cartTable = new TableView<>();
    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();

    private final Label lblSubtotal = new Label("$ 0.00");
    private final Label lblTotal    = new Label("$ 0.00");

    public SalesPosView(ProductDao productDao, VentaDao ventaDao, ProviderDao providerDao) {
        this.productDao = productDao;
        this.ventaDao   = ventaDao;
        this.providerDao= providerDao;
    }

    private boolean isLowStock(org.semilla.app.model.Product p) {
        double s = p.stockActual != null ? p.stockActual : 0.0;
        String u = p.unidadBase != null ? p.unidadBase : "un";
        if ("g".equalsIgnoreCase(u)) return s <= 1000.0; // 1 kg
        return s <= 2.0; // 2 unidades
    }

    public void show(Stage stage, String title) {
        stage.setTitle(title);

        // --- Top: buscador + botón Productos ---
        Label lblSearch = new Label("Código o nombre:");
        tfSearch.setPromptText("Escaneá o escribí el nombre y presioná Enter");

        Button btnOpenProductos = new Button("Productos (F6)");
        btnOpenProductos.setOnAction(e -> openProducts());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(10, lblSearch, tfSearch, spacer, btnOpenProductos);
        top.setPadding(new Insets(10));
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        // --- Panel de resultados ---
        lvResults.setPlaceholder(new Label("Sin resultados"));
        lvResults.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String pv = p.precioVenta != null ? String.format("$ %.2f", p.precioVenta) : "-";
                    String unidad = p.unidadBase != null ? p.unidadBase : "";
                    String pvLabel = "g".equalsIgnoreCase(unidad) ? "(x100g)" : "(un)";
                    String stockTxt = "";
                    if (p.stockActual != null) {
                        stockTxt = "g".equalsIgnoreCase(unidad)
                                ? String.format(" · stock %.0f g", p.stockActual)
                                : String.format(" · stock %.0f un", p.stockActual);
                    }
                    boolean low = isLowStock(p);
                    String warn = low ? "   ⚠ bajo stock" : "";

                    setText(p.nombre + "  ·  " + pv + " " + pvLabel + "  ·  " + unidad + stockTxt + warn);
                    setStyle(low ? "-fx-text-fill: #b54708;" : ""); // color ámbar para advertencia
                }
            }
        });
        lvResults.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Product sel = lvResults.getSelectionModel().getSelectedItem();
                if (sel != null) addProductFlow(sel);
            }
        });

        // --- Carrito ---
        var colNombre  = new TableColumn<CartItem, String>("Nombre");
        var colCodigo  = new TableColumn<CartItem, String>("Código");
        var colCant    = new TableColumn<CartItem, String>("Cant.");
        var colUnidad  = new TableColumn<CartItem, String>("Unidad");
        var colPV      = new TableColumn<CartItem, Double>("PV (un / x100g)");
        var colSubtot  = new TableColumn<CartItem, Double>("Subtotal");

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarra"));
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidadStr"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadBase"));
        colPV.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colSubtot.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colNombre.setPrefWidth(280);
        colCodigo.setPrefWidth(140);
        colCant.setPrefWidth(90);
        colUnidad.setPrefWidth(80);
        colPV.setPrefWidth(110);
        colSubtot.setPrefWidth(110);

        cartTable.getColumns().setAll(colNombre, colCodigo, colCant, colUnidad, colPV, colSubtot);
        cartTable.setItems(cart);
        cartTable.setPlaceholder(new Label("Carrito vacío"));

        // Botonera carrito
        Button btnMas    = new Button("+1");
        Button btnMenos  = new Button("−1");
        Button btnCant   = new Button("Cambiar cantidad");
        Button btnQuitar = new Button("Quitar");
        Button btnNueva  = new Button("Nueva venta (F2)");
        Button btnFin    = new Button("Finalizar venta (F4)");

        btnMas.setOnAction(e -> incSelected(1.0));
        btnMenos.setOnAction(e -> incSelected(-1.0));
        btnCant.setOnAction(e -> changeQtySelected());
        btnQuitar.setOnAction(e -> removeSelected());
        btnNueva.setOnAction(e -> newSale());
        btnFin.setOnAction(e -> finalizeSale());

        HBox buttons = new HBox(10, btnMas, btnMenos, btnCant, btnQuitar, btnNueva, btnFin);
        buttons.setAlignment(Pos.CENTER_LEFT);

        // Totales
        GridPane totals = new GridPane();
        totals.setHgap(10); totals.setVgap(8);
        totals.add(new Label("Subtotal:"), 0, 0);
        totals.add(lblSubtotal, 1, 0);
        totals.add(new Label("Total:"), 0, 1);
        totals.add(lblTotal, 1, 1);
        GridPane.setHgrow(lblSubtotal, Priority.ALWAYS);
        GridPane.setHgrow(lblTotal, Priority.ALWAYS);

        VBox right = new VBox(8, cartTable, buttons, new Separator(), totals);
        right.setPadding(new Insets(10));
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        // Layout base: resultados a la izquierda, carrito a la derecha
        VBox left = new VBox(8, new Label("Resultados"), lvResults);
        left.setPadding(new Insets(10));
        VBox.setVgrow(lvResults, Priority.ALWAYS);

        SplitPane center = new SplitPane(left, right);
        center.setDividerPositions(0.35);

        BorderPane root = new BorderPane(center, top, null, null, null);
        Scene scene = new Scene(root, 1200, 650);
        stage.setScene(scene);
        stage.show();

        // Atajos
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.F2) newSale();
            if (ev.getCode() == KeyCode.F3) { tfSearch.requestFocus(); tfSearch.selectAll(); }
            if (ev.getCode() == KeyCode.F4) finalizeSale();
            if (ev.getCode() == KeyCode.F6) openProducts();                  // NUEVO
            if (ev.getCode() == KeyCode.DELETE) removeSelected();
            if (ev.getCode() == KeyCode.ENTER && lvResults.isFocused()) {
                Product sel = lvResults.getSelectionModel().getSelectedItem();
                if (sel != null) addProductFlow(sel);
            }
        });

        // Buscar al tipear
        tfSearch.textProperty().addListener((obs, o, q) -> searchAsync(q));
        // Enter en buscador: si es numérico => código de barras; si no, agrega selección
        tfSearch.setOnAction(e -> onSearchEnter());

        // Foco inicial
        Platform.runLater(() -> { tfSearch.requestFocus(); tfSearch.selectAll(); });
    }

    // --- abrir ventana de Productos ---
    private void openProducts() {
        try {
            new ProductsView(productDao, providerDao).show(new Stage());
        } catch (Exception ex) {
            showError("No se pudo abrir Productos: " + ex.getMessage());
        }
    }

    // --- Lógica de búsqueda ---

    private void searchAsync(String q) {
        String s = normalize(q).trim();
        if (s.length() < 2) {
            lvResults.getItems().clear();
            return;
        }
        try {
            List<Product> found = productDao.searchByName(s, 50); // contiene (case-insensitive)
            lvResults.getItems().setAll(found);
            if (!found.isEmpty()) lvResults.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            showError("No se pudo buscar: " + ex.getMessage());
        }
    }

    private void onSearchEnter() {
        String raw = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        if (raw.isEmpty()) return;

        // Si es numérico: intentar por código de barras
        if (raw.matches("\\d+")) {
            try {
                Product p = productDao.findByBarcode(raw);
                if (p != null) {
                    addProductFlow(p);
                    tfSearch.clear();
                    return;
                }
            } catch (Exception ex) {
                showError("Búsqueda por código falló: " + ex.getMessage());
                return;
            }
            showInfo("Código no encontrado.");
            return;
        }

        // Si no es numérico: usar la selección de resultados
        Product sel = lvResults.getSelectionModel().getSelectedItem();
        if (sel != null) {
            addProductFlow(sel);
            tfSearch.clear();
        } else {
            showInfo("Elegí un producto de la lista.");
        }
    }

    // --- Carrito ---

    private void addProductFlow(Product p) {
        if ("UNIDAD".equalsIgnoreCase(p.tipo)) {
            addOrInc(p, 1.0);
        } else if ("GRANEL".equalsIgnoreCase(p.tipo)) {
            Double g = askGrams(100.0);
            if (g != null && g > 0) addOrInc(p, g);
        } else {
            addOrInc(p, 1.0);
        }
        tfSearch.requestFocus();
        tfSearch.clear();
    }

    private void addOrInc(Product p, double qty) {
        for (CartItem it : cart) {
            if (it.getProductId().equals(p.id)) {
                it.add(qty);
                cartTable.refresh();
                recalc();
                return;
            }
        }
        CartItem it = new CartItem(p, qty);
        cart.add(it);
        recalc();
    }

    private void incSelected(double delta) {
        CartItem it = cartTable.getSelectionModel().getSelectedItem();
        if (it == null) return;
        if ("un".equalsIgnoreCase(it.getUnidadBase())) {
            it.add(delta);
            if (it.getCantidad() <= 0) cart.remove(it);
        } else {
            it.add(delta * 10.0); // granel: paso rápido 10 g
            if (it.getCantidad() <= 0) cart.remove(it);
        }
        cartTable.refresh();
        recalc();
    }

    private void changeQtySelected() {
        CartItem it = cartTable.getSelectionModel().getSelectedItem();
        if (it == null) return;
        if ("un".equalsIgnoreCase(it.getUnidadBase())) {
            Double u = askUnits(Math.max(1, (int)Math.round(it.getCantidad())));
            if (u != null) it.setCantidad(u);
        } else {
            Double g = askGrams(it.getCantidad());
            if (g != null) it.setCantidad(g);
        }
        cartTable.refresh();
        recalc();
    }

    private void removeSelected() {
        CartItem it = cartTable.getSelectionModel().getSelectedItem();
        if (it != null) { cart.remove(it); recalc(); }
    }

    private void newSale() {
        cart.clear();
        recalc();
        tfSearch.requestFocus();
        tfSearch.clear();
    }

    private void finalizeSale() {
        if (cart.isEmpty()) { showInfo("No hay items en el carrito."); return; }

        // total actual
        double total = 0.0;
        for (CartItem it : cart) total += it.getSubtotal();

        // observación opcional
        TextInputDialog d = new TextInputDialog("");
        d.setTitle("Finalizar venta");
        d.setHeaderText("Observación (opcional)");
        d.setContentText("Observación:");
        Optional<String> ro = d.showAndWait();
        if (ro.isEmpty()) return;
        String obs = ro.get();

        try {
            // mapear items a DTO del DAO
            List<VentaDao.VentaItem> items = new ArrayList<>();
            for (CartItem it : cart) {
                items.add(new VentaDao.VentaItem(
                        it.getProductId(),
                        it.getNombre(),
                        it.getUnidadBase(),
                        it.getPrecioVenta(),
                        it.getCantidad(),
                        it.getSubtotal()
                ));
            }
            int saleId = ventaDao.saveSale(total, obs, items);

            // Descontar stock: UNIDAD resta unidades; GRANEL resta gramos
            for (CartItem it : cart) {
                double delta = - it.getCantidad(); // unidades o gramos
                try { productDao.addStock(it.getProductId(), delta); }
                catch (Exception ex2) { ex2.printStackTrace(); }
            }

            showInfo("Venta guardada. Nro: " + saleId + "\nTotal: $" + String.format("%.2f", total));
            newSale();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("No se pudo guardar la venta: " + ex.getMessage());
        }
    }

    private void recalc() {
        double subtotal = 0.0;
        for (CartItem it : cart) subtotal += it.getSubtotal();
        lblSubtotal.setText(String.format("$ %.2f", subtotal));
        lblTotal.setText(String.format("$ %.2f", subtotal)); // sin descuentos por ahora
    }

    // --- Diálogos de cantidad ---

    private Double askGrams(double defaultGrams) {
        TextInputDialog d = new TextInputDialog(String.valueOf((int)defaultGrams));
        d.setTitle("Cantidad (gr)");
        d.setHeaderText("Ingresá gramos (g)");
        d.setContentText("Gramos:");
        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;
        try {
            double g = Double.parseDouble(r.get().replace(',', '.').trim());
            return g > 0 ? g : null;
        } catch (Exception e) { showInfo("Cantidad inválida."); return null; }
    }

    private Double askUnits(int defaultUnits) {
        TextInputDialog d = new TextInputDialog(String.valueOf(defaultUnits));
        d.setTitle("Cantidad (un)");
        d.setHeaderText("Ingresá unidades");
        d.setContentText("Unidades:");
        Optional<String> r = d.showAndWait();
        if (r.isEmpty()) return null;
        try {
            double u = Double.parseDouble(r.get().replace(',', '.').trim());
            return u > 0 ? (double) Math.round(u) : null; // entero redondeado
        } catch (Exception e) { showInfo("Cantidad inválida."); return null; }
    }

    // --- Helpers UI ---
    private static void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private static void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }

    // --- Item del carrito ---
    public static class CartItem {
        private final Integer productId;
        private final String  nombre;
        private final String  codigoBarra;
        private final String  unidadBase; // "un" / "g"
        private final double  precioVenta; // si unidad = 'g', es precio por 100 g
        private double cantidad; // unidades o gramos

        public CartItem(Product p, double cantidad) {
            this.productId  = p.id;
            this.nombre     = p.nombre;
            this.codigoBarra= p.codigoBarra;
            this.unidadBase = p.unidadBase != null ? p.unidadBase : "un";
            this.precioVenta= p.precioVenta != null ? p.precioVenta : 0.0;
            this.cantidad   = cantidad;
        }

        public void add(double delta) { this.cantidad += delta; }
        public Integer getProductId() { return productId; }
        public String  getNombre()    { return nombre; }
        public String  getCodigoBarra(){ return codigoBarra; }
        public String  getUnidadBase(){ return unidadBase; }
        public Double  getPrecioVenta(){ return round2(precioVenta); }
        public Double  getCantidad()  { return cantidad; }

        public String  getCantidadStr(){
            if (isGranel()) return String.format("%.0f", cantidad); // gramos
            return String.valueOf((int)Math.round(cantidad));        // unidades
        }

        public void   setCantidad(double q){ this.cantidad = q; }

        // Subtotal:
        // - UNIDAD: PV * unidades
        // - GRANEL: PV(x100g) * (gramos / 100)
        public Double getSubtotal()  {
            double factor = isGranel() ? (cantidad / 100.0) : cantidad;
            return round2(precioVenta * factor);
        }

        private boolean isGranel() { return "g".equalsIgnoreCase(unidadBase); }
        private static double round2(double v){ return Math.round(v * 100.0)/100.0; }
    }
}
