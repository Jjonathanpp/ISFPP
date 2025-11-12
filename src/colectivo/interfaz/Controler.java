package colectivo.interfaz;

import colectivo.aplicacion.Configuracion;
import colectivo.aplicacion.Coordinador;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Controler {

    private static final Logger LOGGER = Logger.getLogger(Controler.class);
    private final VistaInterfaz vista;
    private final Coordinador cordinador;
    private final Configuracion configuracion = Configuracion.getInstance();
    private final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    public Controler(VistaInterfaz vista, Coordinador cordinador) {
        this.vista = vista;
        this.cordinador = cordinador;
    }

    public void inicializar() {
        Map<Integer, Parada> paradas = cordinador.listarParadas();
        vista.setOrigenes(paradas);
        vista.setDestinos(paradas);
        vista.configurarComboBox();

        vista.getBtnBuscar().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    Parada origen = vista.getCbOrigen().getValue();
                    Parada destino = vista.getCbDestino().getValue();
                    Integer dia = vista.getDiaSeleccionado();
                    String diaTexto = vista.getCbDia().getValue();
                    String horaStr = vista.getTxtHora().getText();

                    LocalTime hora = null;
                    if (horaStr != null && !horaStr.isBlank()) {
                        try {
                            hora = LocalTime.parse(horaStr, HORA_FORMATO);
                        } catch (Exception e) {
                            vista.setEstado(tr("error.invalidTime"));
                            return;
                        }
                    }
                    if (origen == null || destino == null || dia == null) {
                        vista.setEstado(tr("error.completeAllFields"));
                        return;
                    }
                    LOGGER.info("Búsqueda iniciada: origen=" + origen + ", destino=" + destino + ", día=" + diaTexto + ", hora=" + hora);
                    cordinador.desacopladorLogica(origen, destino, dia, hora);
                    LOGGER.info("Búsqueda completada con éxito.");
                } catch (Exception e) {
                    vista.setEstado(tr("error.searchRoutes", e.getMessage()));
                    LOGGER.error("Error al procesar la búsqueda de recorridos", e);
                }
            }
        });

    }

    public void buscarYMostrar(List<List<Recorrido>> rutas, Parada origen, Parada destino, LocalTime hora) {
        LOGGER.info("Se encontraron " + (rutas == null ? 0 : rutas.size()) + " rutas posibles.");
        vista.setEstado(tr("status.routesFound", rutas == null ? 0 : rutas.size()));
        mostrarRutasEnDialogo(origen, destino, hora, rutas);
    }

    private void mostrarRutasEnDialogo(Parada origen,
                                       Parada destino,
                                       LocalTime horaLlegadaParada,
                                       List<List<Recorrido>> rutas) {
        String texto = formatearRutas(origen, destino, horaLlegadaParada, rutas);
        vista.mostrarMapaRecorrido(origen, destino, rutas);
        vista.mostrarResultadoRutas(texto);
    }

    private String formatearRutas(Parada origen,
                                  Parada destino,
                                  LocalTime horaLlegadaParada,
                                  List<List<Recorrido>> rutas) {
        ResourceBundle bundle = configuracion.getBundle();
        StringBuilder sb = new StringBuilder();

        sb.append(MessageFormat.format(bundle.getString("route.origin"), origen)).append("\n");
        sb.append(MessageFormat.format(bundle.getString("route.destination"), destino)).append("\n");
        String llegada = horaLlegadaParada == null ? bundle.getString("route.noTime") : horaLlegadaParada.toString();
        sb.append(MessageFormat.format(bundle.getString("route.arrivalAtStop"), llegada)).append("\n\n");

        if (rutas == null || rutas.isEmpty()) {
            sb.append(bundle.getString("route.noRoutes")).append("\n");
            return sb.toString();
        }

        int nroRuta = 1;
        for (List<Recorrido> ruta : rutas) {
            String tipo = determinarTipoRuta(ruta, bundle);
            sb.append(MessageFormat.format(bundle.getString("route.routeHeader"), nroRuta++, tipo)).append("\n");

            int totalSeg = 0;
            LocalTime reloj = horaLlegadaParada;
            if (reloj == null && !ruta.isEmpty() && ruta.get(0).getHoraSalida() != null) {
                reloj = ruta.get(0).getHoraSalida();
            }

            for (Recorrido r : ruta) {
                LocalTime hs = r.getHoraSalida();
                if (reloj != null && hs != null && hs.isAfter(reloj)) {
                    int espera = (int) Duration.between(reloj, hs).getSeconds();
                    totalSeg += Math.max(0, espera);
                    reloj = hs;
                }

                int segTramo = Math.max(0, r.getDuracion());
                totalSeg += segTramo;
                if (reloj != null) {
                    reloj = reloj.plusSeconds(segTramo);
                }

                sb.append(formatearRecorrido(r, bundle)).append("\n");
                sb.append(bundle.getString("route.separator")).append("\n");
            }

            sb.append(MessageFormat.format(bundle.getString("route.totalDuration"), Tiempo.segundosATiempo(totalSeg)));
            if (reloj != null) {
                sb.append(MessageFormat.format(bundle.getString("route.arrivalTimeSuffix"), reloj));
            } else if (horaLlegadaParada != null) {
                sb.append(MessageFormat.format(bundle.getString("route.arrivalTimeSuffix"), horaLlegadaParada.plusSeconds(totalSeg)));
            }
            sb.append("\n\n");
        }

        return sb.toString();
    }

    private String formatearRecorrido(Recorrido r, ResourceBundle bundle) {
        boolean caminando = (r.getLinea() == null);

        StringBuilder sb = new StringBuilder();
        if (caminando) {
            sb.append(bundle.getString("route.walking"));
        } else {
            sb.append(MessageFormat.format(bundle.getString("route.line"), r.getLinea().getCodigo()));
        }
        sb.append("\n");

        List<Parada> ps = r.getParadas();
        String paradasTxt = (ps == null || ps.isEmpty())
                ? bundle.getString("route.noStops")
                : String.join(" -> ", ps.stream().map(Parada::toString).toList());
        sb.append(MessageFormat.format(bundle.getString("route.stops"), paradasTxt)).append("\n");

        String horaSalida = (r.getHoraSalida() == null) ? bundle.getString("route.noTime") : r.getHoraSalida().toString();
        sb.append(MessageFormat.format(bundle.getString("route.departure"), horaSalida)).append("\n");

        int durSeg = Math.max(0, r.getDuracion());
        sb.append(MessageFormat.format(bundle.getString("route.duration"), Tiempo.segundosATiempo(durSeg)));

        return sb.toString();
    }

    private String determinarTipoRuta(List<Recorrido> ruta, ResourceBundle bundle) {
        if (ruta == null || ruta.isEmpty()) return bundle.getString("route.type.none");
        boolean algunCaminado = ruta.stream().anyMatch(r -> r.getLinea() == null);
        if (algunCaminado) return bundle.getString("route.type.walkingConnection");
        return (ruta.size() == 1) ? bundle.getString("route.type.direct") : bundle.getString("route.type.connection");
    }

    public void construirVista() {
        getVista().construirVista();
    }

    public VistaInterfaz getVista() {
        return vista;
    }

    private String tr(String key, Object... args) {
        String pattern = configuracion.getString(key);
        return MessageFormat.format(pattern, args);
    }
}
