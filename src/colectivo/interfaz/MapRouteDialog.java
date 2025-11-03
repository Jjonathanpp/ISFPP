package colectivo.interfaz;

import com.sothawo.mapjfx.Configuration;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.Extent;
import com.sothawo.mapjfx.MapLabel;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.Projection;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import javafx.concurrent.Task;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.log4j.Logger;

/**
 * Componente que muestra un mapa y dibuja las rutas (recorridos) entre paradas.
 * Explicación básica:
 * - Esta clase contiene un mapa (mapView) y funciones para dibujar rutas entre paradas.
 * - Recibe origen, destino y una lista de rutas; construye líneas y marcadores y las muestra en el mapa.
 * - Para evitar bloqueos de la interfaz, prepara la geometría en segundo plano (Task) y luego la dibuja en el hilo UI.
 */
public class MapRouteDialog {

    // Logger: sirve para escribir mensajes que ayudan a entender lo que ocurre cuando la app corre.
    private static final Logger LOGGER = Logger.getLogger(MapRouteDialog.class);

    // Mapa visual donde pintamos marcadores y rutas.
    private final MapView mapView = new MapView();

    // Guardamos lo que añadimos al mapa para poder quitarlo después con clearMap().
    private final List<Marker> activeMarkers = new ArrayList<>(); // pines (origen/destino)
    private final List<MapLabel> activeLabels = new ArrayList<>(); // etiquetas de texto junto a pines
    private final List<CoordinateLine> activeLines = new ArrayList<>(); // líneas que representan rutas

    // Contenedor que se entrega a la UI principal para insertar el mapa en la ventana.
    private final BorderPane container = new BorderPane();

    // Bundle de textos para i18n (idiomas). Se usa para etiquetas como "Origen"/"Destino".
    private ResourceBundle bundle;

    // Si pedimos dibujar antes de que el mapa esté listo, guardamos la acción aquí y la ejecutamos luego.
    private Runnable pendingUpdate;

    // Guardamos referencias a métodos de estilo (si existen) usando reflexión.
    // Esto evita romper la compilación si la versión de MapJFX no tiene esos métodos.
    private final Method strokeColorMethod;
    private final Method strokeWidthDoubleMethod;
    private final Method strokeWidthIntMethod;

    // Colores para alternar entre rutas (si la librería permite colorear líneas)
    private static final Color[] ROUTE_COLORS = new Color[] {
            Color.DARKBLUE,
            Color.CRIMSON,
            Color.DARKGREEN,
            Color.DARKORANGE,
            Color.MEDIUMPURPLE
    };

    /**
     * Constructor: prepara el mapa y detecta capacidades (como setters de color/anchura).
     * Comentario simple: configura el mapa y guarda referencias a métodos opcionales.
     */
    public MapRouteDialog(ResourceBundle initialBundle) {
        // Evitar logs molestos de la librería de mapas
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        // Guardar el bundle de textos para i18n
        this.bundle = initialBundle;

        // Inicializar mapa con configuración básica (proyección y controles)
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());
        mapView.setMapType(MapType.OSM);

        // Intentar localizar (una sola vez) métodos que permiten cambiar color/anchura de las líneas.
        // Se hace por reflexión para mantener compatibilidad con distintas versiones de MapJFX.
        Method mColor = null;
        Method mWidthDouble = null;
        Method mWidthInt = null;
        try {
            mColor = CoordinateLine.class.getMethod("setStrokeColor", javafx.scene.paint.Color.class);
        } catch (NoSuchMethodException e) {
            LOGGER.debug("CoordinateLine.setStrokeColor not available");
        }
        try {
            mWidthDouble = CoordinateLine.class.getMethod("setStrokeWidth", double.class);
        } catch (NoSuchMethodException e) {
            try {
                mWidthInt = CoordinateLine.class.getMethod("setStrokeWidth", int.class);
            } catch (NoSuchMethodException ex) {
                LOGGER.debug("CoordinateLine.setStrokeWidth not available");
            }
        }
        this.strokeColorMethod = mColor;
        this.strokeWidthDoubleMethod = mWidthDouble;
        this.strokeWidthIntMethod = mWidthInt;

        // Si el mapa no está listo aún, guardamos lo que haya que ejecutar después.
        mapView.initializedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                if (pendingUpdate != null) {
                    pendingUpdate.run();
                    pendingUpdate = null;
                }
            }
        });

        // Poner el mapView dentro del contenedor para la UI.
        container.setCenter(mapView);
    }

    /**
     * Actualiza el bundle de textos (cuando cambia el idioma).
     */
    public void updateTexts(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Devuelve el contenedor que contiene el mapa para insertarlo en la interfaz.
     */
    public BorderPane getView() {
        return container;
    }

    // Clase auxiliar para devolver, desde la tarea en background, las líneas creadas y las coordenadas
    // que se usarán para ajustar el área visible del mapa (extent).
    private static class LinesResult {
        final List<CoordinateLine> lines;
        final List<Coordinate> extentCoords;
        LinesResult(List<CoordinateLine> lines, List<Coordinate> extentCoords) {
            this.lines = lines;
            this.extentCoords = extentCoords;
        }
    }

    /**
     * showRoutes: método que recibe origen, destino y una lista de rutas y dibuja todo en el mapa.
     * Explicación elemental por pasos (lo que hace internamente):
     * 1) Limpiar lo anterior (clearMap)
     * 2) Añadir marcadores de origen y destino (si tienen coordenadas válidas)
     * 3) Si hay rutas: crear en background (Task) una sola línea por ruta (CoordinateLine)
     *    y aplicar color/anchura si es posible; luego añadir todas las líneas al mapa en el UI thread.
     * 4) Ajustar la vista del mapa para que las rutas y marcadores entren en la pantalla.
     */
    public void showRoutes(Parada origen, Parada destino, List<List<Recorrido>> rutas) {
        Runnable update = () -> {
            // 1) Limpiar lo anterior
            clearMap();

            // Lista de coordenadas que usaremos para centrar/encuadrar el mapa
            List<Coordinate> extentCoordinates = new ArrayList<>();

            // Convertir origen/destino a coordenadas válidas (o null si no tienen lat/lon)
            Coordinate originCoordinate = toCoordinate(origen);
            Coordinate destinationCoordinate = toCoordinate(destino);

            // Si hay coordenadas válidas, añadir marcadores (pins)
            if (originCoordinate != null) {
                addMarker(originCoordinate, getString("view.map.origin"));
                extentCoordinates.add(originCoordinate);
            }
            if (destinationCoordinate != null && (originCoordinate == null || !originCoordinate.equals(destinationCoordinate))) {
                addMarker(destinationCoordinate, getString("view.map.destination"));
                extentCoordinates.add(destinationCoordinate);
            }

            boolean hayRutas = rutas != null && !rutas.isEmpty();

            // Si no hay rutas, solo ajustar vista según marcadores y salir
            if (!hayRutas) {
                if (extentCoordinates.size() >= 2) {
                    mapView.setExtent(Extent.forCoordinates(extentCoordinates));
                } else if (!extentCoordinates.isEmpty()) {
                    mapView.setCenter(extentCoordinates.get(0));
                    mapView.setZoom(14);
                }
                return;
            }

            // 2) Crear las líneas en background para no bloquear la interfaz
            Task<LinesResult> task = new Task<>() {
                @Override
                protected LinesResult call() {
                    List<CoordinateLine> linesToAdd = new ArrayList<>();
                    List<Coordinate> allCoordsForExtent = new ArrayList<>(extentCoordinates);

                    int colorIndex = 0;
                    for (List<Recorrido> ruta : rutas) {
                        Color color = ROUTE_COLORS[colorIndex % ROUTE_COLORS.length];
                        colorIndex++;

                        // Construir la lista ordenada de paradas (origen + paradas intermedias + destino)
                        List<Parada> orderedStops = buildOrderedStops(origen, destino, ruta);
                        List<Coordinate> coords = new ArrayList<>();
                        Coordinate prev = null;
                        for (Parada parada : orderedStops) {
                            // Si la parada es nula o no tiene coordenadas válidas, la saltamos
                            if (parada == null) { prev = null; continue; }
                            Coordinate c = toCoordinate(parada);
                            if (c == null) { prev = null; continue; }
                            // Evitar repetir el mismo punto consecutivo
                            if (prev != null && prev.equals(c)) { prev = c; continue; }
                            coords.add(c);
                            allCoordsForExtent.add(c);
                            prev = c;
                        }

                        // Si la ruta tiene al menos 2 puntos, crear una sola CoordinateLine para toda la ruta
                        if (coords.size() >= 2) {
                            CoordinateLine line = new CoordinateLine(coords);
                            // Intentar aplicar color/anchura si la librería lo permite (reflexión)
                            if (strokeColorMethod != null) {
                                try { strokeColorMethod.invoke(line, color); }
                                catch (IllegalAccessException | InvocationTargetException e) { LOGGER.debug("Failed to invoke setStrokeColor", e); }
                            }
                            if (strokeWidthDoubleMethod != null) {
                                try { strokeWidthDoubleMethod.invoke(line, 4.0); }
                                catch (IllegalAccessException | InvocationTargetException e) { LOGGER.debug("Failed to invoke setStrokeWidth(double)", e); }
                            } else if (strokeWidthIntMethod != null) {
                                try { strokeWidthIntMethod.invoke(line, 4); }
                                catch (IllegalAccessException | InvocationTargetException e) { LOGGER.debug("Failed to invoke setStrokeWidth(int)", e); }
                            }
                            line.setVisible(true);
                            linesToAdd.add(line);
                        }
                    }
                    return new LinesResult(linesToAdd, allCoordsForExtent);
                }
            };

            // 3) Cuando la tarea termine, añadir todas las líneas en el hilo UI
            task.setOnSucceeded(evt -> {
                LinesResult result = task.getValue();
                List<CoordinateLine> lines = result.lines;
                List<Coordinate> extentCoords = result.extentCoords;
                for (CoordinateLine l : lines) {
                    mapView.addCoordinateLine(l);
                    activeLines.add(l);
                }
                // Ajustar encuadre según coordenadas recolectadas
                if (extentCoords.size() >= 2) {
                    mapView.setExtent(Extent.forCoordinates(extentCoords));
                } else if (!extentCoordinates.isEmpty()) {
                    if (extentCoordinates.size() >= 2) {
                        mapView.setExtent(Extent.forCoordinates(extentCoordinates));
                    } else if (!extentCoordinates.isEmpty()) {
                        mapView.setCenter(extentCoordinates.get(0));
                        mapView.setZoom(14);
                    }
                }
            });

            task.setOnFailed(evt -> LOGGER.error("Error building map lines", task.getException()));

            // Ejecutar la tarea en un hilo separado para no bloquear la UI
            new Thread(task, "map-build-lines").start();

        };

        // Si el mapa está listo, ejecutar; si no, guardar como pendiente
        if (mapView.getInitialized()) {
            update.run();
        } else {
            pendingUpdate = update;
        }
    }

    /**
     * Borra todo lo que habíamos dibujado: líneas, marcadores y etiquetas.
     * Es útil para empezar de cero antes de dibujar una nueva búsqueda.
     */
    private void clearMap() {
        for (CoordinateLine line : activeLines) {
            mapView.removeCoordinateLine(line);
        }
        activeLines.clear();
        for (Marker marker : activeMarkers) {
            mapView.removeMarker(marker);
        }
        activeMarkers.clear();
        for (MapLabel label : activeLabels) {
            mapView.removeLabel(label);
        }
        activeLabels.clear();

    }

    /**
     * Añade un marcador (pin) en la coordenada dada y, si hay texto, una etiqueta junto al pin.
     */
    private void addMarker(Coordinate coordinate, String labelText) {
        Marker marker = Marker.createProvided(Marker.Provided.BLUE)
                .setVisible(true)
                .setPosition(coordinate);
        mapView.addMarker(marker);
        activeMarkers.add(marker);

        if (labelText != null && !labelText.isBlank()) {
            MapLabel label = new MapLabel(labelText)
                    .setVisible(true)
                    .setPosition(coordinate);
            mapView.addLabel(label);
            activeLabels.add(label);
        }
    }

    /**
     * Construye una lista ordenada de paradas que forman la ruta: origen + paradas intermedias + destino.
     * Además elimina duplicados consecutivos para evitar puntos repetidos.
     */
    private List<Parada> buildOrderedStops(Parada origen, Parada destino, List<Recorrido> recorrido) {
        List<Parada> ordered = new ArrayList<>();

        if (origen != null) {
            ordered.add(origen);
        }

        if (recorrido != null) {
            for (Recorrido r : recorrido) {
                ordered.addAll(r.getParadas());
            }
        }

        if (destino != null) {
            ordered.add(destino);
        }


        List<Parada> filtered = new ArrayList<>();
        Parada previous = null;
        for (Parada parada : ordered) {
            if (parada == null) {
                continue;
            }
            if (previous != null && parada.equals(previous)) {
                previous = parada;
                continue;
            }
            filtered.add(parada);
            previous = parada;
        }
        return filtered;
    }


    private Coordinate toCoordinate(Parada parada) {
        if (parada == null) {
            return null;
        }
        if (Double.isNaN(parada.getLatitud()) || Double.isNaN(parada.getLongitud())) {
            return null;
        }
        return new Coordinate(parada.getLatitud(), parada.getLongitud());
    }
    private String getString(String key) {
        if (bundle != null) {
            try {
                return bundle.getString(key);
            } catch (Exception ignored) {
                // Si no se encuentra la clave, se devuelve la clave tal cual.
            }
        }
        return key;
    }
}
