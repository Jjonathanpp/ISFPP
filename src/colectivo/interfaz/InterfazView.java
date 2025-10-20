package colectivo.interfaz;

import colectivo.modelo.Parada;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class InterfazView {

    private final ComboBox<Parada> cbOrigen  = new ComboBox<>();
    private final ComboBox<Parada> cbDestino = new ComboBox<>();
    private final ComboBox<String> cbDia     = new ComboBox<>();
    private final TextField txtHora          = new TextField();
    private final Button btnBuscar           = new Button("Buscar recorridos");
    private final Label lblEstado            = new Label();
    private final VBox root;

    public InterfazView() {
        Label titulo = new Label("🚌 Consulta de Recorridos");
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        cbDia.getItems().addAll("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo");
        txtHora.setPromptText("HH:mm (ej: 10:35)");
        btnBuscar.setPrefWidth(200);
        btnBuscar.setDefaultButton(true);

        root = new VBox(12,
                titulo,
                new Label("Origen:"),  cbOrigen,
                new Label("Destino:"), cbDestino,
                new Label("Día de la semana:"), cbDia,
                new Label("Hora de llegada:"), txtHora,
                btnBuscar,
                lblEstado
        );
        root.setPadding(new Insets(20));
    }

    // Getters para el controlador
    public Parent getRoot() { return root; }
    public ComboBox<Parada> getCbOrigen() { return cbOrigen; }
    public ComboBox<Parada> getCbDestino() { return cbDestino; }
    public ComboBox<String> getCbDia() { return cbDia; }
    public TextField getTxtHora() { return txtHora; }
    public Button getBtnBuscar() { return btnBuscar; }
    public Label getLblEstado() { return lblEstado; }
}
