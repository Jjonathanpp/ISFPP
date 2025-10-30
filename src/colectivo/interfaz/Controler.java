package colectivo.interfaz;

import colectivo.aplicacion.Coordinador;
import colectivo.logica.Calculo;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.apache.log4j.Logger;


import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controler (controlador de la vista). Ahora hace internamente:
 *  - poblar combos
 *  - registrar listener del botón Buscar
 *  - parsear hora y delegar a buscar(...)
 */
public class Controler {

    private static final Logger LOGGER = Logger.getLogger(Controler.class);


    private final VistaInterfaz vista;
    private final Coordinador cordinador;

    //Formato de la hora
    private final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    public Controler(VistaInterfaz vista, Coordinador cordinador) {
        this.vista = vista;
        this.cordinador = cordinador;
    }

    public void inicializar() {

        //Inicializa la vista, pobla los combos desde el coordinador
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
                    String dia = vista.getCbDia().getValue();
                    String horaStr = vista.getTxtHora().getText();

                    LocalTime hora = null;
                    if(horaStr != null && !horaStr.isBlank()){
                        try {
                            hora = LocalTime.parse(horaStr, HORA_FORMATO);
                        } catch (Exception e) {
                            vista.setEstado("Formato de hora inválido. Use HH:mm (ej. 10:35"); //Hacerlo multi-lenguaje
                            return;
                        }
                    }
                    if (origen == null || destino == null || dia == null) {
                        vista.setEstado("Por favor, complete todos los campos."); //Hacerlo multi-lenguaje
                        return;
                    }
                    LOGGER.info("Búsqueda iniciada: origen=" + origen + ", destino=" + destino + ", día=" + dia + ", hora=" + hora);
                    cordinador.desacopladorLogica(origen, destino, convertirIntaDia(dia), hora);
                    LOGGER.info("Búsqueda completada con éxito.");

                } catch (Exception e) {
                    vista.setEstado("Error al buscar rutas: " + e.getMessage()); //Hacerlo multi-lenguaje
                    LOGGER.error("Error al procesar la búsqueda de recorridos", e);
                }
            }
        });
    }

    private int convertirIntaDia(String dia) { //CREO que hay que hacerlo multi-lenguaje
        return switch (dia) {
            case "Lunes" -> 1;
            case "Martes" -> 2;
            case "Miércoles" -> 3;
            case "Jueves" -> 4;
            case "Viernes" -> 5;
            case "Sábado" -> 6;
            case "Domingo" -> 7;
            default -> -1;
        };
    }

    //====================== SALIDA ====================

    /*
    Este metodo tendria que pasar a ser publico para que coordinador lo llame y le mande toda la data que tomo agarrada de calulo
     */
    public void buscarYMostrar(List<List<Recorrido>> rutas, Parada origen, Parada destino, LocalTime hora) {
        LOGGER.info("Se encontraron " + (rutas == null ? 0 : rutas.size()) + " rutas posibles.");

        // Actualizar estado breve
        vista.setEstado("Rutas encontradas: " + (rutas == null ? 0 : rutas.size()));

        // Mostrar diálogo con detalle (como en tu versión anterior)
        mostrarRutasEnDialogo(origen, destino, hora, rutas);
    }

    private void mostrarRutasEnDialogo(Parada origen,
                                       Parada destino,
                                       LocalTime horaLlegadaParada,
                                       List<List<Recorrido>> rutas) {
        String texto = formatearRutas(origen, destino, horaLlegadaParada, rutas);
        vista.mostrarResultadoRutas(texto);
    }

    private String formatearRutas(Parada origen,
                                  Parada destino,
                                  LocalTime horaLlegadaParada,
                                  List<List<Recorrido>> rutas) {
        StringBuilder sb = new StringBuilder();

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

                sb.append(formatearRecorrido(r)).append("\n");
                sb.append("============================\n");
            }

            sb.append("Duración total: ").append(Tiempo.segundosATiempo(totalSeg));
            if (reloj != null) {
                sb.append("  /  Hora de llegada: ").append(reloj);
            } else if (horaLlegadaParada != null) {
                sb.append("  /  Hora de llegada: ").append(horaLlegadaParada.plusSeconds(totalSeg));
            }
            sb.append("\n\n");
        }

        return sb.toString();
    }

    private String formatearRecorrido(Recorrido r) {
        boolean caminando = (r.getLinea() == null);

        StringBuilder sb = new StringBuilder();
        if (caminando) {
            sb.append("Caminado");
        } else {
            sb.append("Línea: ").append(r.getLinea().getCodigo());
        }
        sb.append("\n");

        List<Parada> ps = r.getParadas();
        String paradasTxt = (ps == null || ps.isEmpty())
                ? "(sin paradas)"
                : String.join(" -> ", ps.stream().map(Parada::toString).toList());
        sb.append("Paradas: ").append(paradasTxt).append("\n");

        String horaSalida = (r.getHoraSalida() == null) ? "--:--" : r.getHoraSalida().toString();
        sb.append("Hora de salida: ").append(horaSalida).append("\n");

        int durSeg = Math.max(0, r.getDuracion());
        sb.append("Duración: ").append(Tiempo.segundosATiempo(durSeg));

        return sb.toString();
    }

    private String determinarTipoRuta(List<Recorrido> ruta) {
        if (ruta == null || ruta.isEmpty()) return "Sin recorrido";
        boolean algunCaminado = ruta.stream().anyMatch(r -> r.getLinea() == null);
        if (algunCaminado) return "Conexión caminando";
        return (ruta.size() == 1) ? "Directo" : "Con conexión";
    }

    //=====================Inicializar====================

    public void construirVista() {
        getVista().construirVista();
    }
    public VistaInterfaz getVista() {
        return vista;
    }


}
