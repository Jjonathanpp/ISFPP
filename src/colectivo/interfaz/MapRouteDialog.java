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

public class MapRouteDialog {

    private static final Logger LOGGER = Logger.getLogger(MapRouteDialog.class);
    private final MapView mapView = new MapView();
    private final List<Marker> activeMarkers = new ArrayList<>();
    private final List<MapLabel> activeLabels = new ArrayList<>();
    private final List<CoordinateLine> activeLines = new ArrayList<>();
    private final BorderPane container = new BorderPane();
    private ResourceBundle bundle;
    private Runnable pendingUpdate;
    private final Method strokeColorMethod;
    private final Method strokeWidthDoubleMethod;
    private final Method strokeWidthIntMethod;

    private static final Color[] ROUTE_COLORS = new Color[] {
            Color.DARKBLUE,
            Color.CRIMSON,
            Color.DARKGREEN,
            Color.DARKORANGE,
            Color.MEDIUMPURPLE
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

        mapView.initializedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                if (pendingUpdate != null) {
                    pendingUpdate.run();
                    pendingUpdate = null;
                }
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
            if (destinationCoordinate != null && (originCoordinate == null || !originCoordinate.equals(destinationCoordinate))) {
                addMarker(destinationCoordinate, getString("view.map.destination"));
                extentCoordinates.add(destinationCoordinate);
            }

            boolean hayRutas = rutas != null && !rutas.isEmpty();

            if (!hayRutas) {
                if (extentCoordinates.size() >= 2) {
                    mapView.setExtent(Extent.forCoordinates(extentCoordinates));
                } else if (!extentCoordinates.isEmpty()) {
                    mapView.setCenter(extentCoordinates.get(0));
                    mapView.setZoom(14);
                }
                return;
            }

            Task<LinesResult> task = new Task<>() {
                @Override
                protected LinesResult call() {
                    List<CoordinateLine> linesToAdd = new ArrayList<>();
                    List<Coordinate> allCoordsForExtent = new ArrayList<>(extentCoordinates);

                    int colorIndex = 0;
                    for (List<Recorrido> ruta : rutas) {
                        Color color = ROUTE_COLORS[colorIndex % ROUTE_COLORS.length];
                        colorIndex++;

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

                        if (coords.size() >= 2) {
                            CoordinateLine line = new CoordinateLine(coords);
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

            task.setOnSucceeded(evt -> {
                LinesResult result = task.getValue();
                List<CoordinateLine> lines = result.lines;
                List<Coordinate> extentCoords = result.extentCoords;
                for (CoordinateLine l : lines) {
                    mapView.addCoordinateLine(l);
                    activeLines.add(l);
                }
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

            new Thread(task, "map-build-lines").start();

        };

        if (mapView.getInitialized()) {
            update.run();
        } else {
            pendingUpdate = update;
        }
    }

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
            }
        }
        return key;
    }
}
