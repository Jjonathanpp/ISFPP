package colectivo.interfaz;

import colectivo.modelo.Parada;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;
import java.util.ResourceBundle;

public class ParadaDialog {
    private final Dialog<Parada> dialog;
    private final TextField txtCodigo = new TextField();
    private final TextField txtDireccion = new TextField();
    private final TextField txtLat = new TextField();
    private final TextField txtLon = new TextField();

    public ParadaDialog(Parada existente, ResourceBundle bundle) {
        dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Insertar Parada" : "Actualizar Parada");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Código:"), 0, 0);
        grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Dirección:"), 0, 1);
        grid.add(txtDireccion, 1, 1);
        grid.add(new Label("Latitud:"), 0, 2);
        grid.add(txtLat, 1, 2);
        grid.add(new Label("Longitud:"), 0, 3);
        grid.add(txtLon, 1, 3);

        if (existente != null) {
            txtCodigo.setText(existente.getCodigo());
            txtCodigo.setDisable(true); // no permitir cambiar código primario en actualización (opcional)
            txtDireccion.setText(existente.getDireccion());
            txtLat.setText(String.valueOf(existente.getLatitud()));
            txtLon.setText(String.valueOf(existente.getLongitud()));
        }

        // Validación simple: habilitar OK solo si hay código y dirección
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        txtCodigo.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty() || txtDireccion.getText().trim().isEmpty()));
        txtDireccion.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty() || txtCodigo.getText().trim().isEmpty()));

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String codigo = txtCodigo.getText().trim();
                    String direccion = txtDireccion.getText().trim();
                    double lat = txtLat.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtLat.getText().trim());
                    double lon = txtLon.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtLon.getText().trim());
                    return new Parada(codigo, direccion, lat, lon);
                } catch (NumberFormatException ex) {
                    // mostrar alerta si lat/lon inválidos
                    Alert a = new Alert(Alert.AlertType.ERROR, "Latitud/Longitud inválidas.");
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });
    }

    public Optional<Parada> showAndWait() {
        return dialog.showAndWait();
    }
}
