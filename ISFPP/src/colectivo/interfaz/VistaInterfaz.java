package colectivo.interfaz;

import colectivo.modelo.Parada;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

import java.util.List;

public class VistaInterfaz {

    private final ComboBox<Parada> cbOrigen  = new ComboBox<>();
    private final ComboBox<Parada> cbDestino = new ComboBox<>();
    private final ComboBox<String> cbDia     = new ComboBox<>();
    private final TextField txtHora          = new TextField();
    private final Button btnbtnBuscar        = new Button("Buscar recorridos"); //CAMBIAR PARA QUE SEA ABIERTO A NUEVOS IDIOMAS
    private final Label lblEstado            = new Label();
    private VBox root;

    //Genericos (seguramente un par se tienen que hacer multi-lenguaje
    private Label titulo; //Hacer su getter y setter

    public VistaInterfaz() {
        this.titulo = new Label("🚌 Consulta de Recorridos");
        this.root = new VBox();
    }

    //Build que se llama desde el controlador del MVC
    public void construirVista() {
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");
        cbDia.getItems().addAll("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"); //Hacerlo multi-lenguaje,
                                                                                                        // no se me ocurrio como hacerlo
        txtHora.setPromptText("HH:mm (ej: 10:35)");
        btnbtnBuscar.setPrefWidth(200);
        btnbtnBuscar.setDefaultButton(true);

        root = new VBox(12,
                titulo,
                new Label("Origen:"),  cbOrigen,
                new Label("Destino:"), cbDestino,
                new Label("Día de la semana:"), cbDia,
                new Label("Hora de llegada:"), txtHora,
                btnbtnBuscar,
                lblEstado
        );
        root.setPadding(new Insets(20));
    }

    //Setters que va a usar el controlador del MVC
    public void setOrigenes(List<Parada> origenes) {
        cbOrigen.getItems().clear();
        cbOrigen.getItems().addAll(origenes);
    }
    public void setDestinos(List<Parada> destinos) {
        cbDestino.getItems().clear();
        cbDestino.getItems().addAll(destinos);
    }

    /** Muestra el resultado de las rutas en un cuadro de diálogo */
    public void mostrarResultadoRutas(String texto) {
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(60);
        area.setPrefRowCount(25);

        Alert a = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
        a.setTitle("Resultados");
        a.setHeaderText("Rutas encontradas");
        a.getDialogPane().setContent(area);
        a.showAndWait();
    }

    public Parent getRoot() { return root; }
    public void setEstado(String txt) { lblEstado.setText(txt); }

    //Por si el controlador del MVC necesita acceso directo
    public ComboBox<Parada> getCbOrigen() { return cbOrigen; }
    public ComboBox<Parada> getCbDestino() { return cbDestino; }
    public ComboBox<String> getCbDia() { return cbDia; }
    public TextField getTxtHora() { return txtHora; }
    public Button getBtnBuscar() { return btnbtnBuscar; }
    public Label getLblEstado() { return lblEstado; }

    public  void configurarComboBox() {
        // Ordenar los elementos por dirección (ignorando mayúsculas/minúsculas)
        cbOrigen.getItems().sort(java.util.Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));
        cbDestino.getItems().sort(java.util.Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));

        // Converter: texto mostrado cuando el combo está cerrado
        StringConverter<Parada> conv = new StringConverter<>() {
            @Override
            public String toString(Parada p) {
                return (p == null) ? "" : p.getDireccion();
            }
            @Override
            public Parada fromString(String s) {
                return null; // no editable
            }
        };
        cbOrigen.setConverter(conv);
        cbDestino.setConverter(conv);

        // CellFactory: texto mostrado en el desplegable
        cbOrigen.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Parada p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getDireccion());
            }
        });
        cbDestino.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Parada p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getDireccion());
            }
        });
    }

}
