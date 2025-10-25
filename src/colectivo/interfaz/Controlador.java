package colectivo.interfaz;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.datos.CargarDatos;
import colectivo.datos.CargarParametros;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.logica.Calculo;
import colectivo.modelo.Recorrido;

import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import org.apache.log4j.Logger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import java.util.*;

public class Controlador {

    private static final Logger LOGGER = Logger.getLogger(Controlador.class);

    private final InterfazView view;
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;


    public Controlador(InterfazView view) {
        this.view = view;
    }

    public void init() {
        try {
            // 1 Carga de Datos
//            CargarParametros.parametros();
//            paradas = CargarDatos.cargarParadas(CargarParametros.getArchivoParada());
//            lineas  = CargarDatos.cargarLineas(CargarParametros.getArchivoLinea(),CargarParametros.getArchivoFrecuencia(),paradas );
//            tramos  = CargarDatos.cargarTramos(CargarParametros.getArchivoTramo(), paradas);

            paradas = ((ParadaDAO) Factory.getInstancia("PARADA")).buscarTodos();

            tramos = ((TramoDAO) Factory.getInstancia("TRAMO")).buscarTodos();

            lineas = ((LineaDAO) Factory.getInstancia("LINEA")).buscarTodos();

            // 2 Inicialización de la Interfaz
            var lista = new ArrayList<>(paradas.values());
            lista.sort(Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));
            view.getCbOrigen().setItems(FXCollections.observableArrayList(lista));
            view.getCbDestino().setItems(FXCollections.observableArrayList(lista));

            //convierte los objetos Parada a String (dirección) para mostrarlos en los ComboBox
            StringConverter<Parada> conv = new StringConverter<>() {
                @Override public String toString(Parada p) { return p == null ? "" : p.getDireccion(); }
                @Override public Parada fromString(String s) { return null; }
            };

            view.getCbOrigen().setConverter(conv);
            view.getCbDestino().setConverter(conv);

            //3 Manejo del evento Buscar
            view.getBtnBuscar().setOnAction(e -> onBuscar());

            LOGGER.info("Controlador inicializado con " + paradas.size() + " paradas, "
                    + tramos.size() + " tramos y " + lineas.size() + " líneas");

        } catch (Exception e) {
            LOGGER.error("Error inicializando el controlador de la interfaz", e);
        }
    }

    /**Metodo para buscar rutas*/
    private void onBuscar() {
        Parada origen = view.getCbOrigen().getValue();
        Parada destino = view.getCbDestino().getValue();
        String diaStr = view.getCbDia().getValue();
        String horaStr = view.getTxtHora().getText();

        if (origen == null || destino == null || diaStr == null || horaStr.isBlank()) {
            view.getLblEstado().setText("❗Complete todos los campos.");
            return;
        }

        LocalTime hora = convertirStringAHora(horaStr);
        int dia = ConvertirDia(diaStr);

        // 1) Llamás a tu cálculo (como ya lo hacías)
        List<List<Recorrido>> rutas = Calculo.calcularRecorrido(origen, destino, dia, hora, tramos);

        LOGGER.debug("Búsqueda de rutas " + origen.getCodigo() + " -> " + destino.getCodigo()
                + " para el día " + dia + " a las " + hora + " devolvió "
                + (rutas == null ? 0 : rutas.size()) + " opciones");

        // 2) Mostrás todo en un diálogo scrollable (sin tocar la vista)
        mostrarRutasEnDialogo(rutas);

        // 3) Algún feedback conciso en la etiqueta
        view.getLblEstado().setText("Rutas encontradas: " + (rutas == null ? 0 : rutas.size()));
    }

    /**Metodo para convertir la Hora que está en Sting a LocalTime */
    private LocalTime convertirStringAHora(String horaStr) {
        if (horaStr == null || horaStr.isBlank()) {
            return null;
        }

        try {
            // Espera formato HH:mm, por ejemplo "10:35"
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
            return LocalTime.parse(horaStr.trim(), formato);
        } catch (DateTimeParseException e) {
            LOGGER.warn("Formato de hora inválido: " + horaStr, e);
            return null;
        }
    }

    /**Metodo para convertir el día que está en String a int */
    private int ConvertirDia(String diaStr) {
        return switch (diaStr.toLowerCase()) {
            case "lunes" -> 1;
            case "martes" -> 2;
            case "miércoles" -> 3;
            case "jueves" -> 4;
            case "viernes" -> 5;
            case "sábado" -> 6;
            case "domingo" -> 7;
            default -> -1;
        };
    }


    /** Muestra el listado completo de rutas (lista de listas) en un diálogo */
    private void mostrarRutasEnDialogo(List<List<Recorrido>> rutas) {
        String texto = formatearRutas(rutas);

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

    /** Convierte la lista de listas en un bloque de texto. */
    private String formatearRutas(List<List<Recorrido>> rutas) {
        if (rutas == null || rutas.isEmpty()) {
            return "No se encontraron rutas.";
        }
        StringBuilder sb = new StringBuilder();
        int idxRuta = 1;

        for (List<Recorrido> ruta : rutas) {
            sb.append("Ruta ").append(idxRuta++).append(":\n");

            int idxTramo = 1;
            for (Recorrido r : ruta) {
                sb.append("  ").append(idxTramo++).append(") ")
                        .append(formatearRecorrido(r))
                        .append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Convierte un Recorrido en una línea textual */
    private String formatearRecorrido(Recorrido r) {
        // Línea o “CAMINAR” cuando la línea sea null
        String nombreLinea = (r.getLinea() == null) ? "CAMINAR" : r.getLinea().getCodigo();

        // Paradas: intentamos mostrar dirección; si no, el código
        List<Parada> paradas = r.getParadas(); // asumo que tu clase Recorrido expone esto
        String paradasTxt = (paradas == null || paradas.isEmpty())
                ? "(sin paradas)"
                : String.join(" -> ",
                paradas.stream()
                        .map(p -> (p.getDireccion() != null && !p.getDireccion().isBlank())
                                ? p.getDireccion()
                                : p.getCodigo())
                        .toList()
        );

        // Hora de salida + duración (en minutos)
        String horaSalida = (r.getHoraSalida() == null) ? "--:--" : r.getHoraSalida().toString();
        int durSeg = (r.getDuracion() <= 0) ? 0 : r.getDuracion();
        long min = Math.round(Math.ceil(durSeg / 60.0));

        return String.format("[%s] %s  (sale %s, ~%d min)", nombreLinea, paradasTxt, horaSalida, min);
    }
}

