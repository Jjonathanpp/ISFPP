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
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Ventana auxiliar que muestra un mapa con el recorrido calculado.
 */
public class MapRouteDialog {

    private final Stage stage = new Stage();
    private final MapView mapView = new MapView();
    private final List<Marker> activeMarkers = new ArrayList<>();
    private final List<MapLabel> activeLabels = new ArrayList<>();
    private CoordinateLine activeLine;
    private ResourceBundle bundle;
    private Runnable pendingUpdate;

    public MapRouteDialog(ResourceBundle initialBundle) {
        // Desactiva logs verbosos de MapJFX
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");

        this.bundle = initialBundle;
        stage.setTitle(bundle.getString("view.map.title"));

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

        BorderPane root = new BorderPane(mapView);
        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
    }

    public void updateTexts(ResourceBundle bundle) {
        this.bundle = bundle;
        stage.setTitle(bundle.getString("view.map.title"));
    }

    public void showRoute(Parada origen, Parada destino, List<Recorrido> recorrido) {
        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.toFront();
        }

        Runnable update = () -> {
            clearMap();

            List<Coordinate> coordinates = buildCoordinates(origen, destino, recorrido);
            if (coordinates.isEmpty()) {
                return;
            }

            addMarker(coordinates.get(0), bundle.getString("view.map.origin"));
            Coordinate last = coordinates.get(coordinates.size() - 1);
            if (!coordinates.get(0).equals(last)) {
                addMarker(last, bundle.getString("view.map.destination"));
            }

            boolean hayRecorrido = recorrido != null && !recorrido.isEmpty();

            if (coordinates.size() > 1) {
                if (hayRecorrido) {
                    activeLine = new CoordinateLine(coordinates)
                            //.setStrokeColor(Color.DARKBLUE)
                            //.setStrokeWidth(4)
                            .setVisible(true);
                    mapView.addCoordinateLine(activeLine);
                }

                mapView.setExtent(Extent.forCoordinates(coordinates));
            } else {
                mapView.setCenter(coordinates.get(0));
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
        if (activeLine != null) {
            mapView.removeCoordinateLine(activeLine);
            activeLine = null;
        }
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
            marker.attachLabel(label);
            mapView.addLabel(label);
            activeLabels.add(label);
        }
    }

    private List<Coordinate> buildCoordinates(Parada origen, Parada destino, List<Recorrido> recorrido) {
        List<Coordinate> coordinates = new ArrayList<>();
        appendCoordinate(coordinates, origen);

        if (recorrido != null) {
            for (Recorrido r : recorrido) {
                for (Parada p : r.getParadas()) {
                    appendCoordinate(coordinates, p);
                }
            }
        }

        appendCoordinate(coordinates, destino);

        // elimina duplicados consecutivos
        List<Coordinate> filtered = new ArrayList<>();
        Coordinate previous = null;
        for (Coordinate coordinate : coordinates) {
            if (coordinate == null) {
                continue;
            }
            if (previous == null || !previous.equals(coordinate)) {
                filtered.add(coordinate);
                previous = coordinate;
            }
        }
        return filtered;
    }

    private void appendCoordinate(List<Coordinate> coordinates, Parada parada) {
        if (parada == null) {
            return;
        }
        Coordinate coordinate = toCoordinate(parada);
        if (coordinate != null) {
            coordinates.add(coordinate);
        }
    }

    private Coordinate toCoordinate(Parada parada) {
        if (Double.isNaN(parada.getLatitud()) || Double.isNaN(parada.getLongitud())) {
            return null;
        }
        return new Coordinate(parada.getLatitud(), parada.getLongitud());
    }
}