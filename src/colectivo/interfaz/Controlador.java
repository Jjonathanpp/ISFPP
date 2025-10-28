package colectivo.interfaz;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.logica.Calculo;
import colectivo.modelo.Recorrido;

import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import java.util.*;

public class Controlador {

    private final InterfazView view;
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;

    public Controlador(InterfazView view) {
        this.view = view;
    }

    public void init() {
        try {
            // 1) Carga de Datos (vía DAOs)
            paradas = ((ParadaDAO) Factory.getInstancia("PARADA")).buscarTodos();
            tramos  = ((TramoDAO)  Factory.getInstancia("TRAMO")).buscarTodos();
            lineas  = ((LineaDAO)  Factory.getInstancia("LINEA")).buscarTodos();

            // 2) Inicialización de la Interfaz
            var lista = new ArrayList<>(paradas.values());
            lista.sort(Comparator.comparing(Parada::getDireccion, String.CASE_INSENSITIVE_ORDER));
            view.getCbOrigen().setItems(FXCollections.observableArrayList(lista));
            view.getCbDestino().setItems(FXCollections.observableArrayList(lista));

            // Convertidor para mostrar la dirección en los ComboBox
            StringConverter<Parada> conv = new StringConverter<>() {
                @Override public String toString(Parada p) { return p == null ? "" : p.getDireccion(); }
                @Override public Parada fromString(String s) { return null; }
            };
            view.getCbOrigen().setConverter(conv);
            view.getCbDestino().setConverter(conv);

            // 3) Manejo del evento Buscar
            view.getBtnBuscar().setOnAction(e -> onBuscar());

        } catch (Exception ignored) { }
    }

    /** Maneja la búsqueda de rutas */
    private void onBuscar() {
        Parada origen  = view.getCbOrigen().getValue();
        Parada destino = view.getCbDestino().getValue();
        String diaStr  = view.getCbDia().getValue();
        String horaStr = view.getTxtHora().getText();

        if (origen == null || destino == null || diaStr == null || horaStr.isBlank()) {
            view.getLblEstado().setText("❗Complete todos los campos.");
            return;
        }

        LocalTime hora = convertirStringAHora(horaStr);
        int dia = ConvertirDia(diaStr);

        // 1) Llamada al cálculo
        List<List<Recorrido>> rutas = Calculo.calcularRecorrido(origen, destino, dia, hora, tramos);

        // 2) Mostrar resultados con el nuevo formateo (usa Parada.toString() y corrige esperas)
        mostrarRutasEnDialogo(origen, destino, hora, rutas);

        // 3) Feedback conciso
        view.getLblEstado().setText("Rutas encontradas: " + (rutas == null ? 0 : rutas.size()));
    }

    /** Convierte la hora en String a LocalTime (formato HH:mm) */
    private LocalTime convertirStringAHora(String horaStr) {
        if (horaStr == null || horaStr.isBlank()) return null;
        try {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
            return LocalTime.parse(horaStr.trim(), formato);
        } catch (DateTimeParseException e) {
            System.out.println("Error: formato de hora inválido -> " + horaStr);
            return null;
        }
    }

    /** Convierte el día en String a int (1=Lunes ... 7=Domingo) */
    private int ConvertirDia(String diaStr) {
        return switch (diaStr.toLowerCase()) {
            case "lunes"     -> 1;
            case "martes"    -> 2;
            case "miércoles" -> 3;
            case "jueves"    -> 4;
            case "viernes"   -> 5;
            case "sábado"    -> 6;
            case "domingo"   -> 7;
            default -> -1;
        };
    }

    // =========================
    // FORMATEO DE SALIDA (usa toString() de Parada y suma esperas + duraciones)
    // =========================

    /** Muestra el resultado con encabezado + bloques según el tipo de ruta */
    private void mostrarRutasEnDialogo(Parada origen,
                                       Parada destino,
                                       LocalTime horaLlegadaParada,
                                       List<List<Recorrido>> rutas) {
        String texto = formatearRutas(origen, destino, horaLlegadaParada, rutas);

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

    /** Convierte el resultado (lista de rutas) en bloque de texto con encabezado, bloques por tramo y totales coherentes */
    private String formatearRutas(Parada origen,
                                  Parada destino,
                                  LocalTime horaLlegadaParada,
                                  List<List<Recorrido>> rutas) {
        StringBuilder sb = new StringBuilder();

        // Encabezado (usando toString() de Parada)
        sb.append("Parada origen:  ").append(origen).append("\n");
        sb.append("Parada destino: ").append(destino).append("\n");
        sb.append("Llega a la parada: ")
                .append(horaLlegadaParada == null ? "--:--" : horaLlegadaParada.toString())
                .append("\n\n");

        if (rutas == null || rutas.isEmpty()) {
            sb.append("No hay un recorrido recomendado.\n");
            return sb.toString();
        }

        int nroRuta = 1;
        for (List<Recorrido> ruta : rutas) {
            String tipo = determinarTipoRuta(ruta);
            sb.append("=== Ruta ").append(nroRuta++).append(" (").append(tipo).append(") ===\n");

            int totalSeg = 0;

            // Reloj que avanza con esperas + duraciones
            LocalTime reloj = horaLlegadaParada;
            if (reloj == null && !ruta.isEmpty() && ruta.get(0).getHoraSalida() != null) {
                reloj = ruta.get(0).getHoraSalida(); // fallback si no me dieron la llegada a la parada
            }

            for (Recorrido r : ruta) {
                // Espera previa al tramo (si el tramo sale después del reloj)
                LocalTime hs = r.getHoraSalida();
                if (reloj != null && hs != null && hs.isAfter(reloj)) {
                    int espera = (int) Duration.between(reloj, hs).getSeconds();
                    totalSeg += Math.max(0, espera);
                    reloj = hs;
                }

                // Duración del tramo
                int segTramo = Math.max(0, r.getDuracion());
                totalSeg += segTramo;
                if (reloj != null) {
                    reloj = reloj.plusSeconds(segTramo);
                }

                // Imprimir bloque del tramo
                sb.append(formatearRecorrido(r)).append("\n");
                sb.append("============================\n");
            }

            // Totales de la ruta
            sb.append("Duración total: ").append(formatearDuracion(totalSeg));
            if (reloj != null) {
                sb.append("  /  Hora de llegada: ").append(reloj);
            } else if (horaLlegadaParada != null) {
                sb.append("  /  Hora de llegada: ").append(horaLlegadaParada.plusSeconds(totalSeg));
            }
            sb.append("\n\n");
        }

        return sb.toString();
    }

    /** Usa el toString() de Parada directamente para imprimir las paradas del tramo */
    private String formatearRecorrido(Recorrido r) {
        boolean caminando = (r.getLinea() == null);

        StringBuilder sb = new StringBuilder();
        if (caminando) {
            sb.append("Caminado");
        } else {
            sb.append("Línea: ").append(r.getLinea().getCodigo());
        }
        sb.append("\n");

        // Paradas (usa toString())
        List<Parada> ps = r.getParadas();
        String paradasTxt = (ps == null || ps.isEmpty())
                ? "(sin paradas)"
                : String.join(" -> ", ps.stream().map(Parada::toString).toList());
        sb.append("Paradas: ").append(paradasTxt).append("\n");

        // Hora de salida
        String horaSalida = (r.getHoraSalida() == null) ? "--:--" : r.getHoraSalida().toString();
        sb.append("Hora de salida: ").append(horaSalida).append("\n");

        // Duración del tramo (formateada)
        int durSeg = Math.max(0, r.getDuracion());
        sb.append("Duración: ").append(formatearDuracion(durSeg));

        return sb.toString();
    }

    // =========
    // HELPERS
    // =========

    /** Determina el tipo: Directo / Conexión / Conexión caminando (solo para mostrar en el subtítulo) */
    private String determinarTipoRuta(List<Recorrido> ruta) {
        if (ruta == null || ruta.isEmpty()) return "Sin recorrido";
        boolean algunCaminado = ruta.stream().anyMatch(r -> r.getLinea() == null);
        if (algunCaminado) return "Conexión caminando";
        return (ruta.size() == 1) ? "Directo" : "Con conexión";
    }

    /** Formatea duración en HH:MM o HH:MM:SS si hay segundos no múltiplos de 60 */
    private String formatearDuracion(int totalSeg) {
        if (totalSeg < 0) totalSeg = 0;
        int h = totalSeg / 3600;
        int m = (totalSeg % 3600) / 60;
        int s = totalSeg % 60;

        if (s == 0) {
            return String.format("%02d:%02d", h, m);
        }
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
