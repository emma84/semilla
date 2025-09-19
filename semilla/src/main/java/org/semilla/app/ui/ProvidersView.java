// src/main/java/org/semilla/app/ui/ProvidersView.java
package org.semilla.app.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.semilla.app.dao.ProviderDao;
import org.semilla.app.model.Provider;

public class ProvidersView {
    private final ProviderDao dao;
    private final TableView<Provider> table = new TableView<>();
    private final ObservableList<Provider> data = FXCollections.observableArrayList();

    private final TextField tfId = disabled(new TextField());
    private final TextField tfNombre = new TextField();
    private final TextField tfCUIT = new TextField();
    private final TextField tfTel = new TextField();
    private final TextField tfEmail = new TextField();
    private final TextField tfDir = new TextField();
    private final CheckBox  chkActivo = new CheckBox("Activo");

    public ProvidersView(ProviderDao dao) { this.dao = dao; }

    public void show(Stage owner) throws Exception {
        Stage stage = new Stage();
        stage.setTitle("Semilla · Proveedores");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        var colId = new TableColumn<Provider, Integer>("ID");
        var colNombre = new TableColumn<Provider, String>("Nombre");
        var colTel = new TableColumn<Provider, String>("Teléfono");
        var colEmail = new TableColumn<Provider, String>("Email");
        var colActivo = new TableColumn<Provider, Integer>("Activo");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        colId.setPrefWidth(60); colNombre.setPrefWidth(220); colTel.setPrefWidth(130); colEmail.setPrefWidth(200); colActivo.setPrefWidth(70);

        table.getColumns().addAll(colId, colNombre, colTel, colEmail, colActivo);
        table.setItems(data);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populate(b));

        HBox r1 = row("ID:", tfId, "Nombre:", tfNombre);
        HBox r2 = row("CUIT:", tfCUIT, "Teléfono:", tfTel);
        HBox r3 = row("Email:", tfEmail, "Dirección:", tfDir);
        HBox r4 = new HBox(10, chkActivo); r4.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevo = new Button("Nuevo");
        Button btnGuardar = new Button("Guardar");
        Button btnCerrar = new Button("Cerrar");

        btnNuevo.setOnAction(e -> clear());
        btnGuardar.setOnAction(e -> onSave());
        btnCerrar.setOnAction(e -> stage.close());

        HBox actions = new HBox(10, btnNuevo, btnGuardar, btnCerrar);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(10, r1, r2, r3, r4, actions);
        form.setPadding(new Insets(10));
        form.setPrefWidth(560);

        SplitPane split = new SplitPane(new VBox(table), form);
        split.setDividerPositions(0.55);

        BorderPane root = new BorderPane(split);
        root.setPadding(new Insets(10));

        reload();

        stage.setScene(new Scene(root, 1050, 600));
        stage.showAndWait();
    }

    private void reload() {
        try { data.setAll(dao.findAll()); if (!data.isEmpty()) table.getSelectionModel().selectFirst();
        } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error cargando proveedores: " + ex.getMessage()); }
    }

    private void populate(Provider p) {
        if (p == null) { clear(); return; }
        tfId.setText(p.id != null ? p.id.toString() : "");
        tfNombre.setText(n(p.nombre));
        tfCUIT.setText(n(p.cuit));
        tfTel.setText(n(p.telefono));
        tfEmail.setText(n(p.email));
        tfDir.setText(n(p.direccion));
        chkActivo.setSelected(p.activo == null || p.activo == 1);
    }

    private void clear() {
        tfId.clear(); tfNombre.clear(); tfCUIT.clear(); tfTel.clear(); tfEmail.clear(); tfDir.clear(); chkActivo.setSelected(true);
    }

    private void onSave() {
        try {
            Provider p = new Provider();
            String id = tfId.getText().trim();
            p.id = id.isEmpty() ? null : Integer.parseInt(id);
            p.nombre = tfNombre.getText().trim();
            p.cuit = tfCUIT.getText().trim();
            p.telefono = tfTel.getText().trim();
            p.email = tfEmail.getText().trim();
            p.direccion = tfDir.getText().trim();
            p.activo = chkActivo.isSelected() ? 1 : 0;

            if (p.nombre.isEmpty()) { alert(Alert.AlertType.INFORMATION, "Ingresá un nombre."); return; }

            Integer existing = dao.findIdByNameCI(p.nombre);
            if (p.id == null) {
                if (existing != null) { alert(Alert.AlertType.INFORMATION, "Ya existe un proveedor con ese nombre."); return; }
                int newId = dao.insert(p);
                reload(); selectById(newId);
                alert(Alert.AlertType.INFORMATION, "Proveedor creado (ID=" + newId + ").");
            } else {
                if (existing != null && !existing.equals(p.id)) { alert(Alert.AlertType.INFORMATION, "Ya existe otro proveedor con ese nombre."); return; }
                dao.update(p);
                reload(); selectById(p.id);
                alert(Alert.AlertType.INFORMATION, "Proveedor actualizado.");
            }
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error guardando: " + ex.getMessage());
        }
    }

    private static TextField disabled(TextField tf) { tf.setEditable(false); tf.setDisable(true); return tf; }
    private static HBox row(String l1, Control c1, String l2, Control c2) {
        Label a = new Label(l1); a.setMinWidth(120); Label b = new Label(l2); b.setMinWidth(120);
        HBox hb = new HBox(10, a, c1, b, c2); hb.setAlignment(Pos.CENTER_LEFT); return hb;
    }
    private static void alert(Alert.AlertType t, String m) { new Alert(t, m, ButtonType.OK).showAndWait(); }
    private static String n(String s) { return s == null ? "" : s; }
    private void selectById(int id) {
        for (int i = 0; i < data.size(); i++) {
            Provider p = data.get(i);
            if (p.id != null && p.id == id) { table.getSelectionModel().select(i); table.scrollTo(i); break; }
        }
    }
}
