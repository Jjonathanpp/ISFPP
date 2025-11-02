package colectivo.interfaz;

import colectivo.aplicacion.Configuracion;
import colectivo.aplicacion.Coordinador;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import colectivo.util.Tiempo;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private final Configuracion configuracion = Configuracion.getInstance();

    // Formato de la hora
    private final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    public Controler(VistaInterfaz vista, Coordinador cordinador) {
        this.vista = vista;
        this.cordinador = cordinador;
    }

    public void inicializar() {
        // Inicializa la vista, pobla los combos desde el coordinador
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

        registrarHandlersDML();

    }

    // ====================== SALIDA ======================

    /**
     * Este método tendría que pasar a ser público para que Coordinador lo llame
     * y le mande toda la data obtenida de Calculo.
     */
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

        // Muestra el mapa primero (sin bloquear)
        List<Recorrido> primeraRuta = (rutas == null || rutas.isEmpty()) ? null : rutas.get(0);
        vista.mostrarMapaRecorrido(origen, destino, primeraRuta);

        // Muestra el texto en un diálogo aparte (no modal)
        vista.mostrarResultadoRutasNoBloqueante(texto);
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

    // ===================== Inicializar =====================
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

    //============= botones DML =============

    /**
     * Registra los handlers para los MenuItems / botones de Insertar / Actualizar / Eliminar.
     * Aquí se muestran ejemplos:
     *  - Actualizar Línea: abre un diálogo modal para editar una Linea y luego delega al coordinador/modelo.
     *  - Insertar Línea: abre diálogo para crear nueva Linea.
     *  - Eliminar Línea: muestra confirmación.
     * Hacer lo mismo para Parada y Tramo (ejemplos básicos incluidos).
     */
    private void registrarHandlersDML() {
        // ---------- PARADA ----------
        vista.getMiInsertarParada().setOnAction(e -> {
            ParadaDialog dialog = new ParadaDialog(null, configuracion.getBundle());
            Optional<Parada> res = dialog.showAndWait();
            res.ifPresent(parada -> {
                try {
                    cordinador.getEmpresa().agregarParada(parada);
                    refrescarParadasEnVista();
                    vista.setEstado("Parada insertada: " + parada.getCodigo());
                } catch (Exception ex) {
                    vista.setEstado("Error al insertar parada: " + ex.getMessage());
                    LOGGER.error("Error al insertar parada", ex);
                }
            });
        });

        vista.getMiActualizarParada().setOnAction(e -> {
            Optional<Parada> elegido = elegirParada("Seleccionar parada a actualizar");
            elegido.ifPresent(paradaExistente -> {
                ParadaDialog dialog = new ParadaDialog(paradaExistente, configuracion.getBundle());
                Optional<Parada> res = dialog.showAndWait();
                res.ifPresent(paradaActualizada -> {
                    try {
                        cordinador.getEmpresa().modificarParada(paradaActualizada);
                        refrescarParadasEnVista();
                        vista.setEstado("Parada actualizada: " + paradaActualizada.getCodigo());
                    } catch (Exception ex) {
                        vista.setEstado("Error al actualizar parada: " + ex.getMessage());
                        LOGGER.error("Error al actualizar parada", ex);
                    }
                });
            });
        });

        vista.getMiEliminarParada().setOnAction(e -> {
            Optional<Parada> elegido = elegirParada("Seleccionar parada a eliminar");
            elegido.ifPresent(parada -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                        "¿Eliminar parada " + parada.getCodigo() + " - " + parada.getDireccion() + "?",
                        ButtonType.OK, ButtonType.CANCEL);
                conf.setHeaderText("Confirmar eliminación");
                Optional<ButtonType> r = conf.showAndWait();
                if (r.isPresent() && r.get() == ButtonType.OK) {
                    try {
                        cordinador.getEmpresa().eliminarParada(parada);
                        refrescarParadasEnVista();
                        vista.setEstado("Parada eliminada: " + parada.getCodigo());
                    } catch (Exception ex) {
                        vista.setEstado("Error al eliminar parada: " + ex.getMessage());
                        LOGGER.error("Error al eliminar parada", ex);
                    }
                }
            });
        });
        // ---------- TRAMO ----------
        vista.getMiInsertarTramo().setOnAction(e -> {
            TramoDialog dialog = new TramoDialog(null, cordinador.listarParadas(), configuracion.getBundle());
            Optional<Tramo> res = dialog.showAndWait();
            res.ifPresent(tramo -> {
                try {
                    cordinador.getEmpresa().agregarTramo(tramo);
                    refrescarTramosEnVista();
                    vista.setEstado("Tramo insertado: " + tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo());
                } catch (Exception ex) {
                    vista.setEstado("Error al insertar tramo: " + ex.getMessage());
                    LOGGER.error("Error al insertar tramo", ex);
                }
            });
        });

        vista.getMiActualizarTramo().setOnAction(e -> {
            Optional<Tramo> elegido = elegirTramo("Seleccionar tramo a actualizar");
            elegido.ifPresent(tramoExistente -> {
                TramoDialog dialog = new TramoDialog(tramoExistente, cordinador.listarParadas(), configuracion.getBundle());
                Optional<Tramo> res = dialog.showAndWait();
                res.ifPresent(tramoActualizado -> {
                    try {
                        cordinador.getEmpresa().modificarTramo(tramoActualizado);
                        refrescarTramosEnVista();
                        vista.setEstado("Tramo actualizado: " + tramoActualizado.getInicio().getCodigo() + "-" + tramoActualizado.getFin().getCodigo());
                    } catch (Exception ex) {
                        vista.setEstado("Error al actualizar tramo: " + ex.getMessage());
                        LOGGER.error("Error al actualizar tramo", ex);
                    }
                });
            });
        });

        vista.getMiEliminarTramo().setOnAction(e -> {
            Optional<Tramo> elegido = elegirTramo("Seleccionar tramo a eliminar");
            elegido.ifPresent(tramo -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                        "¿Eliminar tramo " + tramo.getInicio().getCodigo() + " → " + tramo.getFin().getCodigo() + "?",
                        ButtonType.OK, ButtonType.CANCEL);
                conf.setHeaderText("Confirmar eliminación");
                Optional<ButtonType> r = conf.showAndWait();
                if (r.isPresent() && r.get() == ButtonType.OK) {
                    try {
                        cordinador.getEmpresa().eliminarTramo(tramo);
                        refrescarTramosEnVista();
                        vista.setEstado("Tramo eliminado: " + tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo());
                    } catch (Exception ex) {
                        vista.setEstado("Error al eliminar tramo: " + ex.getMessage());
                        LOGGER.error("Error al eliminar tramo", ex);
                    }
                }
            });
        });

        // ---------- LINEA (patrón) ----------
        vista.getMiInsertarLinea().setOnAction(e -> {
            LineaDialog dialog = new LineaDialog(null, cordinador.listarParadas(), configuracion.getBundle());
            Optional<Linea> res = dialog.showAndWait();
            res.ifPresent(linea -> {
                // Ejecutar en background si la persistencia puede demorar
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        cordinador.getEmpresa().agregarLinea(linea);
                        return null;
                    }
                };
                task.setOnSucceeded(evt -> {
                    refrescarLineasEnVista();
                    vista.setEstado("Línea insertada: " + linea.getCodigo());
                });
                task.setOnFailed(evt -> {
                    Throwable ex = task.getException();
                    vista.setEstado("Error al insertar línea: " + ex.getMessage());
                    LOGGER.error("Error insertar linea", ex);
                });
                new Thread(task, "insert-linea").start();
            });
        });

        vista.getMiActualizarLinea().setOnAction(e -> {
            Optional<Linea> elegido = elegirLinea("Seleccionar línea a actualizar");
            elegido.ifPresent(lineaExistente -> {
                LineaDialog dialog = new LineaDialog(lineaExistente, cordinador.listarParadas(), configuracion.getBundle());
                Optional<Linea> res = dialog.showAndWait();
                res.ifPresent(lineaActualizada -> {
                    Task<Void> task = new Task<>() {
                        @Override protected Void call() throws Exception {
                            cordinador.getEmpresa().modificarLinea(lineaActualizada);
                            return null;
                        }
                    };
                    task.setOnSucceeded(evt -> {
                        refrescarLineasEnVista();
                        vista.setEstado("Línea actualizada: " + lineaActualizada.getCodigo());
                    });
                    task.setOnFailed(evt -> {
                        Throwable ex = task.getException();
                        vista.setEstado("Error al actualizar línea: " + ex.getMessage());
                        LOGGER.error("Error actualizar linea", ex);
                    });
                    new Thread(task, "update-linea").start();
                });
            });
        });

        vista.getMiEliminarLinea().setOnAction(e -> {
            Optional<Linea> elegido = elegirLinea("Seleccionar línea a eliminar");
            elegido.ifPresent(linea -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                        "¿Eliminar línea " + linea.getCodigo() + " - " + linea.getNombre() + "?",
                        ButtonType.OK, ButtonType.CANCEL);
                conf.setHeaderText("Confirmar eliminación");
                Optional<ButtonType> r = conf.showAndWait();
                if (r.isPresent() && r.get() == ButtonType.OK) {
                    Task<Void> task = new Task<>() {
                        @Override protected Void call() throws Exception {
                            cordinador.getEmpresa().eliminarLinea(linea);
                            return null;
                        }
                    };
                    task.setOnSucceeded(evt -> {
                        refrescarLineasEnVista();
                        vista.setEstado("Línea eliminada: " + linea.getCodigo());
                    });
                    task.setOnFailed(evt -> {
                        Throwable ex = task.getException();
                        vista.setEstado("Error al eliminar línea: " + ex.getMessage());
                        LOGGER.error("Error al eliminar línea", ex);
                    });
                    new Thread(task, "delete-linea").start();
                }
            });
        });
    }

    //Helpers
    private Optional<Parada> elegirParada(String titulo) {
        Map<Integer, Parada> mapa = cordinador.listarParadas();
        List<Parada> items = mapa.values().stream().sorted(Comparator.comparing(Parada::getDireccion)).toList();
        ChoiceDialog<Parada> choice = new ChoiceDialog<>(items.isEmpty() ? null : items.get(0), items);
        choice.setTitle(titulo);
        choice.setHeaderText(titulo);
        choice.setContentText("Parada:");
        choice.setContentText(new StringConverter<>() {
            @Override
            public String toString(Object o) {
                Parada p = (Parada) o;
                return p == null ? "" : p.getCodigo() + " - " + p.getDireccion();
            }

            @Override
            public Parada fromString(String string) { return null; }
        }.toString());
        return choice.showAndWait();
    }
    private Optional<Tramo> elegirTramo(String titulo) {
        Map<String, Tramo> mapa = cordinador.listarTramos();
        List<Tramo> items = mapa.values().stream().toList();
        ChoiceDialog<Tramo> choice = new ChoiceDialog<>(items.isEmpty()?null:items.get(0), items);
        choice.setTitle(titulo);
        choice.setHeaderText(titulo);
        choice.setContentText("Tramo:");
        choice.setContentText(new StringConverter<>() {
            @Override
            public String toString(Object o) {
                Tramo t = (Tramo) o;
                if (t == null) return "";
                return t.getInicio().getCodigo() + " → " + t.getFin().getCodigo() + " (t=" + t.getTiempo() + ")";
            }

            @Override
            public Tramo fromString(String string) { return null; }
        }.toString());
        return choice.showAndWait();
    }

    // Refrescar vistas después de cambios
    private void refrescarParadasEnVista() {
        Map<Integer, Parada> p = cordinador.listarParadas();
        vista.setOrigenes(p);
        vista.setDestinos(p);
        vista.configurarComboBox();
    }

    private void refrescarTramosEnVista() {
        // Si tienes componentes que muestran tramos, actualizalos. Aquí sólo registramos log/estado.
        // También puedes lanzar un método en Coordinador para refrescar vista específica.
        LOGGER.info("Refrescando tramos en vista (si corresponde).");
    }

    private Optional<Linea> elegirLinea(String titulo) {
        Map<String, Linea> mapa = cordinador.getEmpresa().getLineas();
        List<Linea> items = mapa.values().stream().sorted(Comparator.comparing(Linea::getCodigo)).toList();
        ChoiceDialog<Linea> choice = new ChoiceDialog<>(items.isEmpty() ? null : items.get(0), items);
        choice.setTitle(titulo);
        choice.setHeaderText(titulo);
        choice.setContentText("Línea:");
        choice.setContentText(new StringConverter<Linea>() {
            @Override
            public String toString(Linea l) {
                return l == null ? "" : l.getCodigo() + " - " + l.getNombre();
            }
            @Override
            public Linea fromString(String string) { return null; }
        }.toString());
        return choice.showAndWait();
    }

    // Refrescar las vistas/estados relativos a líneas (por ahora actualiza habilitación de menús)
    private void refrescarLineasEnVista() {
        Map<String, Linea> lineas = cordinador.getEmpresa().getLineas();
        boolean empty = lineas == null || lineas.isEmpty();
        // Deshabilitar opciones si no hay líneas
        vista.getMiActualizarLinea().setDisable(empty);
        vista.getMiEliminarLinea().setDisable(empty);
        // Si en el futuro hay listados de líneas, actualizar aquí.
        LOGGER.info("Refrescando líneas en vista (habilitaciones).");
    }

    /**
     * El controler va a hacer el manejo de la logica de los botones que creamos en la vista. La logica funciona siendo
     * vista (crea los botones) <-> contoler (maneja la logica de los botones) <->
     * <-> coordinador (Es el que maneja la batuta) <-> Empresa (Va a tocar el DAO) <->
     * <-> DAO (Hace las operaciones en la base de datos / archivos)
     *
     * VistaInterfaz va a construir los tres MenuButtons de insertar, eliminar y actualizar. Cada MenuButton va a tener
     * tres MenuItems (Parada, Tramo, Linea). La vista solo inicializa los botones, el Controler en su metodo inicializar()
     * va a obtener las referencias de esos MenuItems por los getters y le va a dar funcionalidad con setOnAction.
     * Cada MenuItem va a tener su propio handler que va a abrir un Dialogo (creado aparte) para pedir los datos
     * necesarios para cada operacion (insertar, eliminar, actualizar) y luego va a delegar al Coordinador para que haga
     * la operacion correspondiente.
     * Explicación de cada handler:
     * - Insertar Parada: Abre un ParadaDialog para pedir los datos de la nueva parada. Si el usuario confirma, llama a
     *   cordinador.getEmpresa().agregarParada(parada) para insertar la nueva parada.
     * - Actualizar Parada: Muestra un ChoiceDialog para seleccionar la parada a actualizar. Luego abre un ParadaDialog
     *   con los datos de la parada seleccionada. Si el usuario confirma, llama a
     *   cordinador.getEmpresa().modificarParada(paradaActualizada) para actualizar la parada.
     * - Eliminar Parada: Muestra un ChoiceDialog para seleccionar la parada a eliminar. Luego muestra un Alert de confirmacion.
     *   Si el usuario confirma, llama a cordinador.getEmpresa().eliminarParada(parada) para eliminar la parada.
     *
     * - Insertar Tramo: Similar a insertar parada, pero usando TramoDialog y cordinador.getEmpresa().agregarTramo(tramo).
     * - Actualizar Tramo: Similar a actualizar parada, pero usando TramoDialog y cordinador.getEmpresa().modificarTramo(tramoActualizado).
     * - Eliminar Tramo: Similar a eliminar parada, pero usando cordinador.getEmpresa().eliminarTramo(tramo).
     *
     * - Insertar Linea: Similar a insertar parada, pero usando LineaDialog y cordinador.getEmpresa().agregarLinea(linea).
     * - Actualizar Linea: Similar a actualizar parada, pero usando LineaDialog y cordinador.getEmpresa().modificarLinea(lineaActualizada).
     * - Eliminar Linea: Similar a eliminar parada, pero usando cordinador.getEmpresa().eliminarLinea(linea).
     *
     *
     */


}
