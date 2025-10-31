package colectivo.interfaz;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class LineaDialog {
    private final Dialog<Linea> dialog;
    private final TextField txtCodigo = new TextField();
    private final TextField txtNombre = new TextField();

    private final ComboBox<Parada> cbParadasDisponibles = new ComboBox<>();
    private final ListView<Parada> lvParadasSeleccionadas = new ListView<>();
    private final Button btnAgregar = new Button("Agregar →");
    private final Button btnQuitar = new Button("← Quitar");
    private final Button btnSubir = new Button("↑");
    private final Button btnBajar = new Button("↓");

    /**
     * Construye el diálogo.
     * @param existente linea a editar (puede ser null para crear)
     * @param paradasMapa mapa de paradas disponibles (clave Integer)
     * @param bundle resource bundle (puedes usarlo para i18n — actualmente no obligatorio)
     */
    public LineaDialog(Linea existente, Map<Integer, Parada> paradasMapa, ResourceBundle bundle) {
        dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Insertar Línea" : "Actualizar Línea");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // preparar lista de paradas disponibles ordenadas por dirección
        List<Parada> disponibles = paradasMapa == null ? Collections.emptyList()
                : paradasMapa.values().stream().sorted(Comparator.comparing(Parada::getDireccion)).collect(Collectors.toList());
        ObservableList<Parada> disponObs = FXCollections.observableArrayList(disponibles);
        cbParadasDisponibles.setItems(disponObs);
        cbParadasDisponibles.setEditable(false);
        cbParadasDisponibles.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Parada p) { return p == null ? "" : p.getCodigo() + " - " + p.getDireccion(); }
            @Override public Parada fromString(String s) { return null; }
        });

        // lista seleccionada
        ObservableList<Parada> seleccionadas = FXCollections.observableArrayList();
        lvParadasSeleccionadas.setItems(seleccionadas);
        lvParadasSeleccionadas.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Parada p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getCodigo() + " - " + p.getDireccion());
            }
        });

        // Si existe, precargar datos
        if (existente != null) {
            txtCodigo.setText(existente.getCodigo());
            txtCodigo.setDisable(true); // no cambiar código en actualización (opcional)
            txtNombre.setText(existente.getNombre());
            if (existente.getParadas() != null) {
                seleccionadas.addAll(existente.getParadas());
            }
        }

        // Layout: campos + selección de paradas
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Código:"), 0, 0);
        grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);

        // Paradas area: combo + botones + listview + up/down
        VBox vbLeft = new VBox(6, new Label("Paradas disponibles:"), cbParadasDisponibles, btnAgregar);
        VBox vbRight = new VBox(6, new Label("Paradas línea:"), lvParadasSeleccionadas);
        HBox hbMid = new HBox(6, btnQuitar);
        VBox vbOrder = new VBox(6, btnSubir, btnBajar);
        HBox hParadas = new HBox(10, vbLeft, hbMid, vbRight, vbOrder);
        hParadas.setPadding(new Insets(8, 0, 0, 0));

        // agregar todo al grid (paradas ocupan varias filas)
        grid.add(hParadas, 0, 2, 2, 1);

        // Acciones de botones
        btnAgregar.setOnAction(e -> {
            Parada sel = cbParadasDisponibles.getValue();
            if (sel != null && !seleccionadas.contains(sel)) {
                seleccionadas.add(sel);
            }
        });
        btnQuitar.setOnAction(e -> {
            Parada sel = lvParadasSeleccionadas.getSelectionModel().getSelectedItem();
            if (sel != null) seleccionadas.remove(sel);
        });
        btnSubir.setOnAction(e -> {
            int idx = lvParadasSeleccionadas.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Collections.swap(seleccionadas, idx, idx - 1);
                lvParadasSeleccionadas.getSelectionModel().select(idx - 1);
            }
        });
        btnBajar.setOnAction(e -> {
            int idx = lvParadasSeleccionadas.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < seleccionadas.size() - 1) {
                Collections.swap(seleccionadas, idx, idx + 1);
                lvParadasSeleccionadas.getSelectionModel().select(idx + 1);
            }
        });

        // Validación: OK habilitado sólo si código no vacío, nombre no vacío y al menos 2 paradas
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        Runnable validar = () -> {
            boolean validoCodigo = !txtCodigo.getText().trim().isEmpty();
            boolean validoNombre = !txtNombre.getText().trim().isEmpty();
            boolean minimoParadas = seleccionadas.size() >= 2;
            okButton.setDisable(!(validoCodigo && validoNombre && minimoParadas));
        };
        txtCodigo.textProperty().addListener((obs, o, n) -> validar.run());
        txtNombre.textProperty().addListener((obs, o, n) -> validar.run());
        seleccionadas.addListener((ListChangeListener<Parada>) c -> validar.run());

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String codigo = txtCodigo.getText().trim();
                String nombre = txtNombre.getText().trim();
                List<Parada> paradas = new ArrayList<>(seleccionadas);
                return new Linea(codigo, nombre, paradas);
            }
            return null;
        });
    }

    public Optional<Linea> showAndWait() {
        return dialog.showAndWait();
    }
}

/**
 *
 */
