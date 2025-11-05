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
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Muestra un mapa (MapJFX) y dibuja rutas entre paradas.
 * Integra OpenRouteService (ORS) para que el trazo siga calles.
 * Colorea: Directo (1 pierna) con un color fijo y Conexión (2+ piernas) con un color por pierna.
 */
public class MapRouteDialog {

    // === ORS (OpenRouteService) ===
    // Opción segura: tomar de variable de entorno si existe
    private static final String ORS_API_KEY = /*System.getenv("ORS_API_KEY") != null ? System.getenv("ORS_API_KEY") :*/
            "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjU2Y2FiMzcxODVmYTRhY2I5ZDNlZjhjMjFkNmVkYmM0IiwiaCI6Im11cm11cjY0In0=";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Logger
    private static final Logger LOGGER = Logger.getLogger(MapRouteDialog.class);

    // Mapa
    private final MapView mapView = new MapView();

    // Lo que agregamos al mapa (para poder limpiar)
    private final List<Marker> activeMarkers = new ArrayList<>();
    private final List<MapLabel> activeLabels = new ArrayList<>();
    private final List<CoordinateLine> activeLines = new ArrayList<>();

    // Contenedor para insertar el mapa en la UI
    private final BorderPane container = new BorderPane();

    // i18n
    private ResourceBundle bundle;

    // Acciones diferidas hasta que el mapa esté inicializado
    private Runnable pendingUpdate;

    // Métodos opcionales de estilo en CoordinateLine (compatibilidad)
    private final Method strokeColorMethod;
    private final Method strokeWidthDoubleMethod;
    private final Method strokeWidthIntMethod;

    // Colores
    private static final Color DIRECT_COLOR = Color.DODGERBLUE; // Directo (1 pierna)
    private static final Color[] LEG_COLORS = new Color[]{
            Color.CRIMSON, Color.DARKGREEN, Color.DARKORANGE, Color.MEDIUMPURPLE, Color.DARKBLUE
    };

    public MapRouteDialog(ResourceBundle initialBundle) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        this.bundle = initialBundle;

        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());
        mapView.setMapType(MapType.OSM);

        // Reflexión para setear color/anchura si la versión lo soporta
        Method mColor = null, mWidthDouble = null, mWidthInt = null;
        try { mColor = CoordinateLine.class.getMethod("setStrokeColor", javafx.scene.paint.Color.class); }
        catch (NoSuchMethodException ignored) { LOGGER.debug("CoordinateLine.setStrokeColor not available"); }
        try { mWidthDouble = CoordinateLine.class.getMethod("setStrokeWidth", double.class); }
        catch (NoSuchMethodException e) {
            try { mWidthInt = CoordinateLine.class.getMethod("setStrokeWidth", int.class); }
            catch (NoSuchMethodException ignored) { LOGGER.debug("CoordinateLine.setStrokeWidth not available"); }
        }
        this.strokeColorMethod = mColor;
        this.strokeWidthDoubleMethod = mWidthDouble;
        this.strokeWidthIntMethod = mWidthInt;

        mapView.initializedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal) && pendingUpdate != null) {
                pendingUpdate.run();
                pendingUpdate = null;
            }
        });

        container.setCenter(mapView);
    }

    public void updateTexts(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public BorderPane getView() {
        return container;
    }

    // Resultado para pasar del hilo de background al de UI
    private static class LinesResult {
        final List<CoordinateLine> lines;
        final List<Coordinate> extentCoords;
        LinesResult(List<CoordinateLine> lines, List<Coordinate> extentCoords) {
            this.lines = lines;
            this.extentCoords = extentCoords;
        }
    }

    /**
     * Dibuja marcadores y rutas. Para cada alternativa:
     * - Directo (1 Recorrido): una línea con DIRECT_COLOR.
     * - Conexión (2+ Recorridos): una línea por pierna con colores distintos (LEG_COLORS).
     */
    public void showRoutes(Parada origen, Parada destino, List<List<Recorrido>> rutas) {
        Runnable update = () -> {
            clearMap();

            List<Coordinate> extentCoordinates = new ArrayList<>();

            Coordinate originCoordinate = toCoordinate(origen);
            Coordinate destinationCoordinate = toCoordinate(destino);

            if (originCoordinate != null) {
                addMarker(originCoordinate, getString("view.map.origin"));
                extentCoordinates.add(originCoordinate);
            }
            if (destinationCoordinate != null &&
                    (originCoordinate == null || !originCoordinate.equals(destinationCoordinate))) {
                addMarker(destinationCoordinate, getString("view.map.destination"));
                extentCoordinates.add(destinationCoordinate);
            }

            boolean hayRutas = rutas != null && !rutas.isEmpty();
            if (!hayRutas) {
                encuadrar(extentCoordinates);
                return;
            }

            Task<LinesResult> task = new Task<>() {
                @Override
                protected LinesResult call() {
                    List<CoordinateLine> linesToAdd = new ArrayList<>();
                    List<Coordinate> allCoordsForExtent = new ArrayList<>(extentCoordinates);

                    for (List<Recorrido> alternativa : rutas) {
                        int legs = (alternativa != null) ? alternativa.size() : 0;

                        if (legs <= 1) {
                            // ===== Directo (0 o 1 pierna) =====
                            List<Parada> ordered = buildOrderedStops(origen, destino, alternativa);
                            List<Coordinate> waypoints = compactToCoordinates(ordered);
                            if (waypoints.size() >= 2) {
                                List<Coordinate> routed = fetchRoutedSafe(waypoints);
                                allCoordsForExtent.addAll(routed);

                                CoordinateLine line = styledLine(routed, DIRECT_COLOR, 4.0);
                                linesToAdd.add(line);
                            }
                        } else {
                            // ===== Conexión (2+ piernas) =====
                            for (int i = 0; i < legs; i++) {
                                List<Parada> stopsForLeg = buildStopsForLeg(origen, destino, alternativa, i);
                                List<Coordinate> waypoints = compactToCoordinates(stopsForLeg);
                                if (waypoints.size() < 2) continue;

                                List<Coordinate> routed = fetchRoutedSafe(waypoints);
                                allCoordsForExtent.addAll(routed);

                                Color color = LEG_COLORS[i % LEG_COLORS.length];
                                CoordinateLine line = styledLine(routed, color, 4.0);
                                linesToAdd.add(line);
                            }
                        }
                    }
                    return new LinesResult(linesToAdd, allCoordsForExtent);
                }
            };

            task.setOnSucceeded(evt -> {
                LinesResult result = task.getValue();
                for (CoordinateLine l : result.lines) {
                    mapView.addCoordinateLine(l);
                    activeLines.add(l);
                }
                if (!result.extentCoords.isEmpty()) {
                    mapView.setExtent(Extent.forCoordinates(result.extentCoords));
                } else {
                    encuadrar(extentCoordinates);
                }
            });

            task.setOnFailed(evt -> LOGGER.error("Error building map lines", task.getException()));
            new Thread(task, "map-build-lines").start();
        };

        if (mapView.getInitialized()) {
            update.run();
        } else {
            pendingUpdate = update;
        }
    }

    /** Limpia líneas, marcadores y etiquetas del mapa. */
    private void clearMap() {
        for (CoordinateLine line : activeLines) mapView.removeCoordinateLine(line);
        activeLines.clear();
        for (Marker marker : activeMarkers) mapView.removeMarker(marker);
        activeMarkers.clear();
        for (MapLabel label : activeLabels) mapView.removeLabel(label);
        activeLabels.clear();
    }

    /** Agrega un marcador (pin) con etiqueta opcional. */
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

    // ===== Helpers de vista/estilo =====

    private void encuadrar(List<Coordinate> extentCoordinates) {
        if (extentCoordinates.size() >= 2) {
            mapView.setExtent(Extent.forCoordinates(extentCoordinates));
        } else if (!extentCoordinates.isEmpty()) {
            mapView.setCenter(extentCoordinates.get(0));
            mapView.setZoom(14);
        }
    }

    private CoordinateLine styledLine(List<Coordinate> coords, Color color, double width) {
        CoordinateLine line = new CoordinateLine(coords);
        if (strokeColorMethod != null) {
            try { strokeColorMethod.invoke(line, color); }
            catch (IllegalAccessException | InvocationTargetException ignored) {}
        }
        if (strokeWidthDoubleMethod != null) {
            try { strokeWidthDoubleMethod.invoke(line, width); }
            catch (IllegalAccessException | InvocationTargetException ignored) {}
        } else if (strokeWidthIntMethod != null) {
            try { strokeWidthIntMethod.invoke(line, (int)Math.round(width)); }
            catch (IllegalAccessException | InvocationTargetException ignored) {}
        }
        line.setVisible(true);
        return line;
    }

    // ===== Construcción de paradas / waypoints =====

    /** Directo: origen + todas las paradas de la alternativa (si existe) + destino. */
    private List<Parada> buildOrderedStops(Parada origen, Parada destino, List<Recorrido> alternativa) {
        List<Parada> ordered = new ArrayList<>();
        if (origen != null) ordered.add(origen);

        if (alternativa != null && !alternativa.isEmpty()) {
            // Si hay 1 sola pierna, se agregan esas paradas
            if (alternativa.size() == 1 && alternativa.get(0) != null && alternativa.get(0).getParadas() != null) {
                ordered.addAll(alternativa.get(0).getParadas());
            }
        }

        if (destino != null) ordered.add(destino);
        return filterConsecutiveDuplicates(ordered);
    }

    /**
     * Conexión: arma las paradas para la pierna i de la alternativa.
     * i==0: incluye origen (si existe) + paradas de la primera pierna.
     * i==last: paradas de la última pierna + destino (si existe).
     * intermedias: solo paradas de esa pierna.
     */
    private List<Parada> buildStopsForLeg(Parada origen, Parada destino, List<Recorrido> alternativa, int i) {
        List<Parada> list = new ArrayList<>();
        Recorrido pierna = alternativa.get(i);

        if (i == 0 && origen != null) list.add(origen);
        if (pierna != null && pierna.getParadas() != null) list.addAll(pierna.getParadas());
        if (i == alternativa.size() - 1 && destino != null) list.add(destino);

        return filterConsecutiveDuplicates(list);
    }

    private List<Parada> filterConsecutiveDuplicates(List<Parada> ordered) {
        List<Parada> filtered = new ArrayList<>();
        Parada previous = null;
        for (Parada p : ordered) {
            if (p == null) continue;
            if (previous != null && p.equals(previous)) { previous = p; continue; }
            filtered.add(p);
            previous = p;
        }
        return filtered;
    }

    /** Convierte paradas a Coordinates (lat,lon), quitando nulos y duplicados consecutivos. */
    private List<Coordinate> compactToCoordinates(List<Parada> paradas) {
        List<Coordinate> waypoints = new ArrayList<>();
        Coordinate prev = null;
        for (Parada p : paradas) {
            Coordinate c = toCoordinate(p);
            if (c == null) continue;
            if (prev != null && prev.equals(c)) continue;
            waypoints.add(c);
            prev = c;
        }
        return waypoints;
    }

    /** Convierte Parada (lat,lon) a Coordinate. */
    private Coordinate toCoordinate(Parada parada) {
        if (parada == null) return null;
        if (Double.isNaN(parada.getLatitud()) || Double.isNaN(parada.getLongitud())) return null;
        return new Coordinate(parada.getLatitud(), parada.getLongitud()); // MapJFX: (lat, lon)
    }

    /** i18n safe-get */
    private String getString(String key) {
        if (bundle != null) {
            try { return bundle.getString(key); } catch (Exception ignored) {}
        }
        return key;
    }

    // ======================
    //  ORS: llamada y parseo
    // ======================

    /** Llama ORS y devuelve polilínea que sigue calles. Si falla, devuelve los waypoints. */
    private List<Coordinate> fetchRoutedSafe(List<Coordinate> waypoints) {
        try { return fetchRoutedGeometryORS(waypoints); }
        catch (Exception e) {
            LOGGER.warn("Fallo ORS, se usa línea directa. Motivo: " + e.getMessage());
            return waypoints;
        }
    }

    /**
     * Pide a ORS la geometría de la ruta (siguiendo calles) para los waypoints dados.
     * Recibe Coordinates (lat,lon) y devuelve Coordinates (lat,lon) de la polilínea resultante.
     */
    private List<Coordinate> fetchRoutedGeometryORS(List<Coordinate> waypoints) throws Exception {
        if (waypoints == null || waypoints.size() < 2) return List.of();
        if (ORS_API_KEY == null || ORS_API_KEY.isBlank()) {
            throw new IllegalStateException("Falta ORS_API_KEY");
        }

        // ORS espera [lon, lat]
        JSONArray coords = new JSONArray();
        for (Coordinate c : waypoints) {
            coords.put(new JSONArray().put(c.getLongitude()).put(c.getLatitude()));
        }

        JSONObject body = new JSONObject()
                .put("coordinates", coords)
                .put("instructions", false)
                .put("geometry_simplify", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ORS_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", ORS_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("ORS error " + resp.statusCode() + ": " + resp.body());
        }

        JSONObject root = new JSONObject(resp.body());
        JSONArray features = root.getJSONArray("features");
        if (features.isEmpty()) return List.of();

        JSONObject geom = features.getJSONObject(0).getJSONObject("geometry");
        JSONArray line = geom.getJSONArray("coordinates");

        List<Coordinate> routed = new ArrayList<>(line.length());
        for (int i = 0; i < line.length(); i++) {
            JSONArray p = line.getJSONArray(i);
            double lon = p.getDouble(0);
            double lat = p.getDouble(1);
            routed.add(new Coordinate(lat, lon)); // MapJFX: (lat, lon)
        }
        return routed;
    }
}
