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
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Componente reutilizable que muestra un mapa con los recorridos calculados.
 */
public class MapRouteDialog {

    private final MapView mapView = new MapView();
    private final List<Marker> activeMarkers = new ArrayList<>();
    private final List<MapLabel> activeLabels = new ArrayList<>();
    private final List<CoordinateLine> activeLines = new ArrayList<>();
    private final BorderPane container = new BorderPane();
    private ResourceBundle bundle;
    private Runnable pendingUpdate;

    private static final Color[] ROUTE_COLORS = new Color[] {
            Color.DARKBLUE,
            Color.CRIMSON,
            Color.DARKGREEN,
            Color.DARKORANGE,
            Color.MEDIUMPURPLE
    };

    public MapRouteDialog(ResourceBundle initialBundle) {
        // Desactiva logs verbosos de MapJFX
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        this.bundle = initialBundle;

        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());
        mapView.setMapType(MapType.OSM);

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

            if (hayRutas) {
                int colorIndex = 0;
                for (List<Recorrido> ruta : rutas) {
                    Color color = ROUTE_COLORS[colorIndex % ROUTE_COLORS.length];
                    colorIndex++;
                    List<Parada> orderedStops = buildOrderedStops(origen, destino, ruta);
                    Coordinate previous = null;

                    for (Parada parada : orderedStops) {
                        if (parada == null) {
                            previous = null;
                            continue;
                        }

                        Coordinate coordinate = toCoordinate(parada);
                        if (coordinate == null) {
                            previous = null;
                            continue;
                        }

                        extentCoordinates.add(coordinate);


                        if (previous != null && !previous.equals(coordinate)) {
                            CoordinateLine segment = new CoordinateLine(List.of(previous, coordinate))
                                    //.setStrokeColor(color)
                                    //.setStrokeWidth(4)
                                    .setVisible(true);
                            mapView.addCoordinateLine(segment);
                            activeLines.add(segment);
                        }

                        previous = coordinate;
                    }
                }
            }

            if (extentCoordinates.size() >= 2) {
                mapView.setExtent(Extent.forCoordinates(extentCoordinates));
            } else if (!extentCoordinates.isEmpty()) {
                mapView.setCenter(extentCoordinates.get(0));
                mapView.setZoom(14);
            }
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
                // Si no se encuentra la clave, se devuelve la clave tal cual.
            }
        }
        return key;
    }
}