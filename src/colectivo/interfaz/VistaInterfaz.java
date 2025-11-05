package colectivo.interfaz;

import colectivo.aplicacion.Configuracion;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    private final Button btnBuscar = new Button();
    private final Label lblEstado = new Label();
    private BorderPane root;
    private final MapRouteDialog mapDialog;
    private final TextArea resultadosArea = new TextArea();

    private Label titulo;
    private Label lblIdioma;
    private Label lblOrigen;
    private Label lblDestino;
    private Label lblDiaSemana;
    private Label lblHora;
    private Label lblMapa;
    private Label lblResultados;

    private final Configuracion configuracion = Configuracion.getInstance();
    private final List<LanguageOption> languageOptions = List.of(
            new LanguageOption(new Locale("es"), "language.spanish"),
            new LanguageOption(Locale.ENGLISH, "language.english"),
            new LanguageOption(Locale.ITALIAN, "language.italian")
    );

    public VistaInterfaz() {
        this.titulo = new Label();
        this.root = new BorderPane();
        this.mapDialog = new MapRouteDialog(configuracion.getBundle());
    }

    public void construirVista() {
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        lblIdioma = new Label();
        lblOrigen = new Label();
        lblDestino = new Label();
        lblDiaSemana = new Label();
        lblHora = new Label();
        lblMapa = new Label();
        lblResultados = new Label();

        configurarSelectorIdiomas();

        HBox selectorIdioma = new HBox(8, lblIdioma, cbIdioma);
        selectorIdioma.setSpacing(8);

        VBox formulario = new VBox(12,
                selectorIdioma,
                titulo,
                lblOrigen, cbOrigen,
                lblDestino, cbDestino,
                lblDiaSemana, cbDia,
                lblHora, txtHora,
                btnBuscar,
                lblEstado
        );
        formulario.setPadding(new Insets(20));
        formulario.setPrefWidth(320);

        Parent mapView = mapDialog.getView();
        VBox mapaContainer = new VBox(10, lblMapa, mapView);
        mapaContainer.setPadding(new Insets(20));
        VBox.setVgrow(mapView, Priority.ALWAYS);

        resultadosArea.setEditable(false);
        resultadosArea.setWrapText(true);
        resultadosArea.setPrefRowCount(12);
        VBox resultadosContainer = new VBox(8, lblResultados, resultadosArea);
        resultadosContainer.setPadding(new Insets(0, 20, 20, 20));

        root = new BorderPane();
        root.setLeft(formulario);
        root.setCenter(mapaContainer);
        root.setBottom(resultadosContainer);

        actualizarTextos(configuracion.getBundle());

        configuracion.localeProperty().addListener((obs, oldLocale, newLocale) -> {
            actualizarTextos(configuracion.getBundle());
            seleccionarIdioma(newLocale);
        });
        seleccionarIdioma(configuracion.getLocale());
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
        titulo.setText(getString(bundle, "view.title"));
        lblIdioma.setText(getString(bundle, "view.languageLabel"));
        lblOrigen.setText(getString(bundle, "view.origin"));
        lblDestino.setText(getString(bundle, "view.destination"));
        lblDiaSemana.setText(getString(bundle, "view.weekday"));
        lblHora.setText(getString(bundle, "view.arrivalTime"));
        lblMapa.setText(getString(bundle, "view.map.title", configuracion.getString("dialog.results.title")));
        lblResultados.setText(getString(bundle, "view.results.title", configuracion.getString("dialog.results.title")));

        btnBuscar.setText(getString(bundle, "view.searchButton"));
        btnBuscar.setPrefWidth(200);
        btnBuscar.setDefaultButton(true);
        txtHora.setPromptText(getString(bundle, "view.timePrompt"));

        resultadosArea.setPromptText(getString(bundle, "view.results.placeholder", configuracion.getString("dialog.results.header")));

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

    private String getString(ResourceBundle bundle, String key) {
        return getString(bundle, key, "!" + key + "!");
    }

    private String getString(ResourceBundle bundle, String key, String fallbackValue) {
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return fallbackValue;
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
        resultadosArea.setText(texto);
        resultadosArea.positionCaret(0);
    }

    public void mostrarMapaRecorrido(Parada origen, Parada destino, List<List<Recorrido>> rutas) {
        mapDialog.showRoutes(origen, destino, rutas);
    }

    public Parent getRoot() { return root; }
    public void setEstado(String txt) { lblEstado.setText(txt); }

    public ComboBox<Parada> getCbOrigen() { return cbOrigen; }
    public ComboBox<Parada> getCbDestino() { return cbDestino; }
    public ComboBox<String> getCbDia() { return cbDia; }
    public TextField getTxtHora() { return txtHora; }
    public Button getBtnBuscar() { return btnBuscar; }

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


}
