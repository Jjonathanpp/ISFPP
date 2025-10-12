package colectivo.interfaz;

import colectivo.datos.CargarDatos;
import colectivo.datos.CargarParametros;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.*;

public class InterfazFX extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1) Cargar nombres de archivos desde /resources/config.properties
            CargarParametros.parametros();

            // 2) Cargar datos reales desde /resources
            Map<Integer, Parada> paradas = CargarDatos.cargarParadas(CargarParametros.getArchivoParada());
            Map<String, Linea> lineas = CargarDatos.cargarLineas(CargarParametros.getArchivoLinea(),CargarParametros.getArchivoFrecuencia(),paradas);
            Map<String, Tramo> tramos = CargarDatos.cargarTramos(CargarParametros.getArchivoTramo(),paradas);

            // 3) Controles base
            Label titulo = new Label("🚌 Consulta de Recorridos");
            titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

            ComboBox<Parada> cbOrigen  = new ComboBox<>();
            ComboBox<Parada> cbDestino = new ComboBox<>();

            ComboBox<String> cbDia = new ComboBox<>();
            cbDia.getItems().addAll("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo");

            TextField txtHora = new TextField();
            txtHora.setPromptText("HH:mm (ej: 10:35)");

            Button btnBuscar = new Button("Buscar recorridos");
            btnBuscar.setPrefWidth(200);

            // Convierte el objeto en String para mostrar en el ComboBox
            StringConverter<Parada> paradaConverter = new StringConverter<>() {
                @Override public String toString(Parada p) {
                    return p == null ? "" : p.getDireccion();
                }
                @Override public Parada fromString(String s) { return null; } // requerido por la interfaz, aunque no se use
            };

            // Celdas del desplegable (lista)
            cbOrigen.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(Parada item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDireccion());
                }
            });
            cbDestino.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(Parada item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDireccion());
                }
            });

            // Aplicar converter a ambos ComboBox
            cbOrigen.setConverter(paradaConverter);
            cbDestino.setConverter(paradaConverter);

            // Cargar y ordenar alfabéticamente por la dirección
            List<Parada> listaOrdenada = new ArrayList<>(paradas.values());
            listaOrdenada.sort(Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));
            cbOrigen.getItems().setAll(listaOrdenada);
            cbDestino.getItems().setAll(listaOrdenada);

            // 4) define como estaran ordenados los elementos en la ventana de la app
            VBox root = new VBox(12,
                    titulo,
                    new Label("Origen:"),  cbOrigen,
                    new Label("Destino:"), cbDestino,
                    new Label("Día de la semana:"), cbDia,
                    new Label("Hora de llegada:"), txtHora,
                    btnBuscar
            );
            root.setPadding(new Insets(20)); //el espacio que hay entre el borde de la ventana y los controles

            stage.setTitle("ISFPP - Interfaz de Recorridos");
            stage.setScene(new Scene(root, 440, 440));
            stage.show();

        } catch (IOException e) {
            showError("Error cargando configuración: " + e.getMessage());
        } catch (Exception e) {
            showError("Error cargando datos: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
