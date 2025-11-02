package colectivo.interfaz;

import colectivo.aplicacion.Configuracion;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;


import java.util.*;

public class VistaInterfaz {

    private static final List<String> DIAS_KEYS = List.of(
            "weekday.monday",
            "weekday.tuesday",
            "weekday.wednesday",
            "weekday.thursday",
            "weekday.friday",
            "weekday.saturday",
            "weekday.sunday"
    );

    private final ComboBox<Parada> cbOrigen = new ComboBox<>();
    private final ComboBox<Parada> cbDestino = new ComboBox<>();
    private final ComboBox<String> cbDia = new ComboBox<>();
    private final ComboBox<LanguageOption> cbIdioma = new ComboBox<>();
    private final TextField txtHora = new TextField();
    //===============DML=================
    private final MenuButton mbInsertar = new MenuButton();
    private final MenuButton mbEliminar = new MenuButton();
    private final MenuButton mbActualizar = new MenuButton();

    private final MenuItem miInsertarParada = new MenuItem();
    private final MenuItem miInsertarLinea = new MenuItem();
    private final MenuItem miInsertarTramo = new MenuItem();

    private final MenuItem miEliminarParada = new MenuItem();
    private final MenuItem miEliminarLinea = new MenuItem();
    private final MenuItem miEliminarTramo = new MenuItem();

    private final MenuItem miActualizarParada = new MenuItem();
    private final MenuItem miActualizarLinea = new MenuItem();
    private final MenuItem miActualizarTramo = new MenuItem();
    private final Button btnBuscar = new Button();
    private final Label lblEstado = new Label();
    private VBox root;
    private final MapRouteDialog mapDialog;

    private Label titulo;
    private Label lblIdioma;
    private Label lblOrigen;
    private Label lblDestino;
    private Label lblDiaSemana;
    private Label lblHora;

    private final Configuracion configuracion = Configuracion.getInstance();
    private final List<LanguageOption> languageOptions = List.of(
            new LanguageOption(new Locale("es"), "language.spanish"),
            new LanguageOption(Locale.ENGLISH, "language.english"),
            new LanguageOption(Locale.ITALIAN, "language.italian")
    );

    public VistaInterfaz() {
        this.titulo = new Label();
        this.root = new VBox();
        this.mapDialog = new MapRouteDialog(configuracion.getBundle());
        configurarMenuButtons();
    }

    public void construirVista() {
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        lblIdioma = new Label();
        lblOrigen = new Label();
        lblDestino = new Label();
        lblDiaSemana = new Label();
        lblHora = new Label();

        configurarSelectorIdiomas();

        HBox selectorIdioma = new HBox(8, lblIdioma, cbIdioma);
        selectorIdioma.setSpacing(8);

        HBox accionesCrud = new HBox(10, mbInsertar, mbActualizar, mbEliminar);
        accionesCrud.setSpacing(10);

        root = new VBox(12,
                selectorIdioma,
                titulo,
                lblOrigen, cbOrigen,
                lblDestino, cbDestino,
                lblDiaSemana, cbDia,
                lblHora, txtHora,
                btnBuscar,
                lblEstado,
                accionesCrud
        );
        root.setPadding(new Insets(20));

        actualizarTextos(configuracion.getBundle());

        configuracion.localeProperty().addListener((obs, oldLocale, newLocale) -> {
            actualizarTextos(configuracion.getBundle());
            seleccionarIdioma(newLocale);
        });
        seleccionarIdioma(configuracion.getLocale());
    }

    private void configurarMenuButtons(){
        mbInsertar.getItems().setAll(
                miInsertarParada,
                miInsertarLinea,
                miInsertarTramo
        );
        mbActualizar.getItems().setAll(
                miActualizarParada,
                miActualizarLinea,
                miActualizarTramo
        );
        mbEliminar.getItems().setAll(
                miEliminarParada,
                miEliminarLinea,
                miEliminarTramo
        );
    }


    private void configurarSelectorIdiomas() {
        cbIdioma.getItems().setAll(languageOptions);
        cbIdioma.valueProperty().addListener((obs, oldOption, newOption) -> {
            if (newOption != null) {
                configuracion.setLocale(newOption.locale());
            }
        });
    }

    private void actualizarTextos(ResourceBundle bundle) {
        titulo.setText(bundle.getString("view.title"));
        lblIdioma.setText(bundle.getString("view.languageLabel"));
        lblOrigen.setText(bundle.getString("view.origin"));
        lblDestino.setText(bundle.getString("view.destination"));
        lblDiaSemana.setText(bundle.getString("view.weekday"));
        lblHora.setText(bundle.getString("view.arrivalTime"));

        btnBuscar.setText(bundle.getString("view.searchButton"));
        btnBuscar.setPrefWidth(200);
        btnBuscar.setDefaultButton(true);
        txtHora.setPromptText(bundle.getString("view.timePrompt"));

        mbInsertar.setText(bundle.getString("view.insert"));
        mbActualizar.setText(bundle.getString("view.update"));
        mbEliminar.setText(bundle.getString("view.delete"));

        miInsertarLinea.setText(bundle.getString("entity.linea"));
        miInsertarParada.setText(bundle.getString("entity.parada"));
        miInsertarTramo.setText(bundle.getString("entity.tramo"));

        miActualizarLinea.setText(bundle.getString("entity.linea"));
        miActualizarParada.setText(bundle.getString("entity.parada"));
        miActualizarTramo.setText(bundle.getString("entity.tramo"));

        miEliminarLinea.setText(bundle.getString("entity.linea"));
        miEliminarParada.setText(bundle.getString("entity.parada"));
        miEliminarTramo.setText(bundle.getString("entity.tramo"));

        int selectedIndex = cbDia.getSelectionModel().getSelectedIndex();
        List<String> nuevosDias = new ArrayList<>();
        for (String key : DIAS_KEYS) {
            nuevosDias.add(bundle.getString(key));
        }
        cbDia.getItems().setAll(nuevosDias);
        if (selectedIndex >= 0 && selectedIndex < cbDia.getItems().size()) {
            cbDia.getSelectionModel().select(selectedIndex);
        }

        configurarComboIdioma(bundle);
        mapDialog.updateTexts(bundle);
    }

    private void configurarComboIdioma(ResourceBundle bundle) {
        StringConverter<LanguageOption> converter = new StringConverter<>() {
            @Override
            public String toString(LanguageOption option) {
                if (option == null) return "";
                return bundle.getString(option.bundleKey());
            }

            @Override
            public LanguageOption fromString(String string) {
                return null;
            }
        };

        cbIdioma.setConverter(converter);
        cbIdioma.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LanguageOption option, boolean empty) {
                super.updateItem(option, empty);
                setText(empty || option == null ? "" : bundle.getString(option.bundleKey()));
            }
        });

        if (cbIdioma.getValue() != null) {
            cbIdioma.setValue(cbIdioma.getValue());
        }
    }

    private void seleccionarIdioma(Locale locale) {
        if (locale == null) return;
        for (LanguageOption option : languageOptions) {
            if (option.locale().getLanguage().equals(locale.getLanguage())) {
                cbIdioma.setValue(option);
                return;
            }
        }
    }

    public void setOrigenes(Map<Integer, Parada> origenes) {
        cbOrigen.getItems().clear();
        List<Parada> listaOrigenes = origenes.values().stream().toList();
        cbOrigen.getItems().addAll(listaOrigenes);
    }

    public void setDestinos(Map<Integer, Parada> destinos) {
        cbDestino.getItems().clear();
        List<Parada> listaDestinos = destinos.values().stream().toList();
        cbDestino.getItems().addAll(listaDestinos);
    }

    public void mostrarResultadoRutas(String texto) {
        ResourceBundle bundle = configuracion.getBundle();
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(60);
        area.setPrefRowCount(25);

        ButtonType okButton = new ButtonType(bundle.getString("dialog.ok"), ButtonBar.ButtonData.OK_DONE);
        Alert a = new Alert(Alert.AlertType.INFORMATION, "", okButton);
        a.setTitle(bundle.getString("dialog.results.title"));
        a.setHeaderText(bundle.getString("dialog.results.header"));
        a.getDialogPane().setContent(area);
        a.showAndWait();
    }

    public void mostrarMapaRecorrido(Parada origen, Parada destino, List<Recorrido> recorrido) {
        mapDialog.showRoute(origen, destino, recorrido);
    }

    public Parent getRoot() { return root; }
    public void setEstado(String txt) { lblEstado.setText(txt); }

    public ComboBox<Parada> getCbOrigen() { return cbOrigen; }
    public ComboBox<Parada> getCbDestino() { return cbDestino; }
    public ComboBox<String> getCbDia() { return cbDia; }
    public TextField getTxtHora() { return txtHora; }
    public Button getBtnBuscar() { return btnBuscar; }
    public Label getLblEstado() { return lblEstado; }

    public MenuItem getMiInsertarParada() { return miInsertarParada; }
    public MenuItem getMiInsertarLinea() { return miInsertarLinea; }
    public MenuItem getMiInsertarTramo() { return miInsertarTramo; }
    public MenuItem getMiEliminarParada() { return miEliminarParada; }
    public MenuItem getMiEliminarLinea() { return miEliminarLinea; }
    public MenuItem getMiEliminarTramo() { return miEliminarTramo; }
    public MenuItem getMiActualizarParada() { return miActualizarParada; }
    public MenuItem getMiActualizarLinea() { return miActualizarLinea; }
    public MenuItem getMiActualizarTramo() { return miActualizarTramo; }

    public Integer getDiaSeleccionado() {
        int index = cbDia.getSelectionModel().getSelectedIndex();
        return index >= 0 ? index + 1 : null;
    }

    public void configurarComboBox() {
        cbOrigen.getItems().sort(java.util.Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));
        cbDestino.getItems().sort(java.util.Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));

        StringConverter<Parada> conv = new StringConverter<>() {
            @Override
            public String toString(Parada p) {
                return (p == null) ? "" : p.getDireccion();
            }
            @Override
            public Parada fromString(String s) {
                return null;
            }
        };
        cbOrigen.setConverter(conv);
        cbDestino.setConverter(conv);

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

    private record LanguageOption(Locale locale, String bundleKey) { }

    /**
     * Las nuevas cosas añadidas son tres menuButtons de insertar, actualizar y eliminar. Son como los combo box.
     * Cada menu button tiene tres menu items: parada, linea y tramo.
     * Puse getters para cada menu item para que el controlador pueda acceder a ellos y asignarles acciones.
     */

    public void mostrarResultadoRutasNoBloqueante(String texto) {
        ResourceBundle bundle = configuracion.getBundle();
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(60);
        area.setPrefRowCount(25);

        Stage stage = new Stage();
        stage.setTitle(bundle.getString("dialog.results.title"));
        stage.setScene(new Scene(area, 600, 400));
        stage.show(); // No bloquea el hilo principal
    }

}
