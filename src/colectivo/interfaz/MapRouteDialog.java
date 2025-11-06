package colectivo.interfaz;

import com.sothawo.mapjfx.*;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import javafx.concurrent.Task;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class MapRouteDialog {

    private static final String ORS_API_KEY =
            "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjU2Y2FiMzcxODVmYTRhY2I5ZDNlZjhjMjFkNmVkYmM0IiwiaCI6Im11cm11cjY0In0=";
    private static final String ORS_URL = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Logger
    private static final Logger LOGGER = Logger.getLogger(MapRouteDialog.class);

    // Mapa y estructuras
    private final MapView mapView = new MapView();
    private final List<Marker> activeMarkers = new ArrayList<>();
    private final List<MapLabel> activeLabels = new ArrayList<>();
    private final List<CoordinateLine> activeLines = new ArrayList<>();
    private final BorderPane container = new BorderPane();

    private ResourceBundle bundle;
    private Runnable pendingUpdate;

    // Métodos de tu versión de MapJFX
    private final Method lineColorMethod;
    private final Method lineWidthIntMethod;

    // Colores usados
    private static final Color DIRECT_COLOR = Color.DODGERBLUE;
    private static final Color[] LEG_COLORS = new Color[]{
            Color.RED, Color.DARKGREEN, Color.ORANGE
    };

    public MapRouteDialog(ResourceBundle initialBundle) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        this.bundle = initialBundle;

        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());
        mapView.setMapType(MapType.OSM);

        Method mColor = null;
        try {
            mColor = CoordinateLine.class.getMethod("setColor", javafx.scene.paint.Color.class);
            LOGGER.info("CoordinateLine color method: setColor");
        } catch (NoSuchMethodException e) {
            LOGGER.warn("No se encontró setColor(Color) en CoordinateLine; las líneas usarán color por defecto.");
        }
        this.lineColorMethod = mColor;

        Method mWidthI = null;
        try {
            mWidthI = CoordinateLine.class.getMethod("setWidth", int.class);
            LOGGER.info("CoordinateLine width(int) method: setWidth");
        } catch (NoSuchMethodException e) {
            LOGGER.warn("No se encontró setWidth(int) en CoordinateLine; se usará ancho por defecto.");
        }
        this.lineWidthIntMethod = mWidthI;

        mapView.initializedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal) && pendingUpdate != null) {
                pendingUpdate.run();
                pendingUpdate = null;
            }
        });

        container.setCenter(mapView);
    }

    public void updateTexts(ResourceBundle bundle) { this.bundle = bundle; }
    public BorderPane getView() { return container; }

    private static class LinesResult {
        final List<CoordinateLine> lines;
        final List<Coordinate> extentCoords;
        LinesResult(List<CoordinateLine> lines, List<Coordinate> extentCoords) {
            this.lines = lines;
            this.extentCoords = extentCoords;
        }
    }

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

            if (rutas == null || rutas.isEmpty()) {
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

                            List<Parada> ordered = buildOrderedStops(origen, destino, alternativa);
                            List<Coordinate> waypoints = compactToCoordinates(ordered);
                            if (waypoints.size() >= 2) {
                                List<Coordinate> routed = fetchRoutedSafe(waypoints);
                                allCoordsForExtent.addAll(routed);

                                Color color = DIRECT_COLOR;
                                CoordinateLine line = styledLine(routed, color, 4.0);
                                linesToAdd.add(line);
                            }
                        } else {

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

                int index = 1;
                for (CoordinateLine l : result.lines) {
                    mapView.addCoordinateLine(l);
                    activeLines.add(l);

                    String colorNombre = colorRgbaToName(l.getColor().toString());
                    LOGGER.info("Se dibujó la línea #" + index + " en color " + colorNombre);
                    index++;
                }

                if (!result.extentCoords.isEmpty()) {
                    mapView.setExtent(Extent.forCoordinates(result.extentCoords));
                } else {
                    encuadrar(extentCoordinates);
                }
            });

            task.setOnFailed(evt -> LOGGER.error("Error construyendo líneas del mapa", task.getException()));
            new Thread(task, "map-build-lines").start();
        };

        if (mapView.getInitialized()) update.run();
        else pendingUpdate = update;
    }

    private void clearMap() {
        for (CoordinateLine line : activeLines) mapView.removeCoordinateLine(line);
        activeLines.clear();
        for (Marker marker : activeMarkers) mapView.removeMarker(marker);
        activeMarkers.clear();
        for (MapLabel label : activeLabels) mapView.removeLabel(label);
        activeLabels.clear();
    }

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

    private void encuadrar(List<Coordinate> coords) {
        if (coords.size() >= 2) mapView.setExtent(Extent.forCoordinates(coords));
        else if (!coords.isEmpty()) {
            mapView.setCenter(coords.get(0));
            mapView.setZoom(14);
        }
    }

    private CoordinateLine styledLine(List<Coordinate> coords, Color color, double width) {
        CoordinateLine line = new CoordinateLine(coords);

        // Color
        if (lineColorMethod != null) {
            try { lineColorMethod.invoke(line, color); }
            catch (Exception e) { LOGGER.warn("No se pudo aplicar color", e); }
        }

        // Ancho
        if (lineWidthIntMethod != null) {
            try { lineWidthIntMethod.invoke(line, (int)Math.round(width)); }
            catch (Exception e) { LOGGER.warn("No se pudo aplicar ancho", e); }
        }

        line.setVisible(true);
        return line;
    }

    private List<Parada> buildOrderedStops(Parada origen, Parada destino, List<Recorrido> alternativa) {
        List<Parada> ordered = new ArrayList<>();
        if (origen != null) ordered.add(origen);
        if (alternativa != null && !alternativa.isEmpty()) {
            Recorrido unico = alternativa.get(0);
            if (unico != null && unico.getParadas() != null) ordered.addAll(unico.getParadas());
        }
        if (destino != null) ordered.add(destino);
        return filterDuplicates(ordered);
    }

    private List<Parada> buildStopsForLeg(Parada origen, Parada destino, List<Recorrido> alternativa, int i) {
        List<Parada> list = new ArrayList<>();
        Recorrido pierna = alternativa.get(i);
        if (i == 0 && origen != null) list.add(origen);
        if (pierna != null && pierna.getParadas() != null) list.addAll(pierna.getParadas());
        if (i == alternativa.size() - 1 && destino != null) list.add(destino);
        return filterDuplicates(list);
    }

    private List<Parada> filterDuplicates(List<Parada> list) {
        List<Parada> result = new ArrayList<>();
        Parada prev = null;
        for (Parada p : list) {
            if (p == null) continue;
            if (prev != null && p.equals(prev)) continue;
            result.add(p);
            prev = p;
        }
        return result;
    }

    private List<Coordinate> compactToCoordinates(List<Parada> paradas) {
        List<Coordinate> coords = new ArrayList<>();
        Coordinate prev = null;
        for (Parada p : paradas) {
            Coordinate c = toCoordinate(p);
            if (c == null) continue;
            if (prev != null && prev.equals(c)) continue;
            coords.add(c);
            prev = c;
        }
        return coords;
    }

    private Coordinate toCoordinate(Parada parada) {
        if (parada == null) return null;
        if (Double.isNaN(parada.getLatitud()) || Double.isNaN(parada.getLongitud())) return null;
        return new Coordinate(parada.getLatitud(), parada.getLongitud());
    }

    private String getString(String key) {
        if (bundle != null) {
            try { return bundle.getString(key); } catch (Exception ignored) {}
        }
        return key;
    }

    private List<Coordinate> fetchRoutedSafe(List<Coordinate> waypoints) {
        try { return fetchRoutedGeometryORS(waypoints); }
        catch (Exception e) {
            LOGGER.warn("Fallo ORS, usando línea directa: " + e.getMessage());
            return waypoints;
        }
    }

    private List<Coordinate> fetchRoutedGeometryORS(List<Coordinate> waypoints) throws Exception {
        if (waypoints == null || waypoints.size() < 2) return List.of();
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
        if (resp.statusCode() != 200) throw new IllegalStateException("ORS error " + resp.statusCode());

        JSONObject root = new JSONObject(resp.body());
        JSONArray features = root.getJSONArray("features");
        if (features.isEmpty()) return List.of();

        JSONArray line = features.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates");

        List<Coordinate> routed = new ArrayList<>(line.length());
        for (int i = 0; i < line.length(); i++) {
            JSONArray p = line.getJSONArray(i);
            double lon = p.getDouble(0);
            double lat = p.getDouble(1);
            routed.add(new Coordinate(lat, lon));
        }
        return routed;
    }

    private static String colorRgbaToName(String rgbaHex) {
        if (rgbaHex == null) return "Desconocido";
        rgbaHex = rgbaHex.trim().toLowerCase();
        if (rgbaHex.startsWith("0x")) rgbaHex = rgbaHex.substring(2);
        if (rgbaHex.length() != 8) return "Desconocido";

        String rgb = rgbaHex.substring(0, 6).toUpperCase(); // ignoramos alfa

        return switch (rgb) {
            case "FF0000" -> "Rojo";
            case "006400" -> "Verde oscuro";
            case "FFA500" -> "Naranja";
            case "1E90FF" -> "Azul (directo)";
            default -> "Desconocido (" + rgb + ")";
        };
    }
}
