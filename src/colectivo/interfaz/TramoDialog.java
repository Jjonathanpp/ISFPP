package colectivo.interfaz;

import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;
import java.util.stream.Collectors;

public class TramoDialog {
    private final Dialog<Tramo> dialog;
    private final ComboBox<Parada> cbInicio = new ComboBox<>();
    private final ComboBox<Parada> cbFin = new ComboBox<>();
    private final TextField txtTiempo = new TextField();
    private final ComboBox<String> cbTipo = new ComboBox<>();

    public TramoDialog(Tramo existente, Map<Integer, Parada> paradas, ResourceBundle bundle) {
        dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Insertar Tramo" : "Actualizar Tramo");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        List<Parada> lista = paradas.values().stream().sorted(Comparator.comparing(Parada::getDireccion)).collect(Collectors.toList());
        cbInicio.setItems(FXCollections.observableArrayList(lista));
        cbFin.setItems(FXCollections.observableArrayList(lista));

        cbInicio.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Parada p) { return p == null ? "" : p.getCodigo() + " - " + p.getDireccion(); }
            @Override public Parada fromString(String s) { return null; }
        });
        cbFin.setConverter(cbInicio.getConverter());

        cbTipo.getItems().addAll("COLECTIVO", "CAMINANDO"); // o usar tus constantes/labels

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Inicio:"), 0, 0);
        grid.add(cbInicio, 1, 0);
        grid.add(new Label("Fin:"), 0, 1);
        grid.add(cbFin, 1, 1);
        grid.add(new Label("Tiempo (s):"), 0, 2);
        grid.add(txtTiempo, 1, 2);
        grid.add(new Label("Tipo:"), 0, 3);
        grid.add(cbTipo, 1, 3);

        if (existente != null) {
            cbInicio.setValue(existente.getInicio());
            cbFin.setValue(existente.getFin());
            txtTiempo.setText(String.valueOf(existente.getTiempo()));
            cbTipo.setValue(existente.getTipo() == 1 ? "COLECTIVO" : "CAMINANDO");
        }

        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.setDisable(true);
        // habilitar OK cuando haya inicio/fin y tiempo válido
        cbInicio.valueProperty().addListener((obs,o,n)-> validar(ok));
        cbFin.valueProperty().addListener((obs,o,n)-> validar(ok));
        txtTiempo.textProperty().addListener((obs,o,n)-> validar(ok));
        cbTipo.valueProperty().addListener((obs,o,n)-> validar(ok));

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Parada inicio = cbInicio.getValue();
                    Parada fin = cbFin.getValue();
                    int tiempo = Integer.parseInt(txtTiempo.getText().trim());
                    int tipo = "COLECTIVO".equals(cbTipo.getValue()) ? 1 : 2;
                    return new Tramo(tiempo, tipo, inicio, fin);
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Tiempo inválido");
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });
    }

    private void validar(Node ok) {
        boolean valido = cbInicio.getValue() != null && cbFin.getValue() != null && !txtTiempo.getText().trim().isEmpty() && cbTipo.getValue() != null;
        ok.setDisable(!valido);
    }

    public Optional<Tramo> showAndWait() {
        return dialog.showAndWait();
    }
}
