package colectivo.monitor;


import colectivo.util.LoggingConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone JavaFX utility that reads the dedicated Log4j2 error log and
 * displays the entries in a simple table. Launching this class is enough to
 * inspect the most recent problems captured by the main application without
 * running the whole ISFPP UI.
 */
public class ErrorLogViewer extends Application {

    private static final Path ERROR_LOG_PATH = Paths.get("logs", "isfpp-errors.log");
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(?<thread>[^]]+)] (?<level>\\w+) (?<logger>[^-]+) - (?<message>.*)$");
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final ObservableList<LogEntry> entries = FXCollections.observableArrayList();
    private final StringProperty statusMessage = new SimpleStringProperty("Esperando advertencias o errores...");

    private WatchService watchService;
    private ExecutorService watcherExecutor;

    @Override
    public void init() throws Exception {
        Files.createDirectories(ERROR_LOG_PATH.getParent());
        watchService = FileSystems.getDefault().newWatchService();
        ERROR_LOG_PATH.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        watcherExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "error-log-watcher");
            thread.setDaemon(true);
            return thread;
        });
        watcherExecutor.submit(this::watchForChanges);
    }

    @Override
    public void start(Stage stage) {
        TableView<LogEntry> tableView = createTable();

        Label subtitle = new Label("Advertencias y errores recientes detectados por Log4j");
        subtitle.getStyleClass().add("subtitle");

        Button refreshButton = new Button("Actualizar");
        refreshButton.setOnAction(event -> loadEntries());

        HBox infoBar = new HBox(subtitle);
        infoBar.setPadding(new Insets(10, 10, 0, 10));

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(statusMessage);

        ToolBar toolBar = new ToolBar(refreshButton, statusLabel);

        BorderPane root = new BorderPane();
        root.setTop(new BorderPane(infoBar, toolBar, null, null, null));
        root.setCenter(tableView);
        BorderPane.setMargin(tableView, new Insets(10));

        Scene scene = new Scene(root, 900, 400);
        java.net.URL stylesheetUrl = ErrorLogViewer.class.getResource("/colectivo/monitor/error-log-viewer");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        }

        stage.setTitle("Visor de errores ISFPP");
        stage.setScene(scene);
        stage.show();

        loadEntries();
    }

    @Override
    public void stop() throws Exception {
        if (watchService != null) {
            watchService.close();
        }
        if (watcherExecutor != null) {
            watcherExecutor.shutdownNow();
        }
    }

    private TableView<LogEntry> createTable() {
        TableView<LogEntry> tableView = new TableView<>(entries);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LogEntry, String> timeColumn = new TableColumn<>("Fecha");
        timeColumn.setCellValueFactory(data -> data.getValue().timestampProperty());

        TableColumn<LogEntry, String> levelColumn = new TableColumn<>("Nivel");
        levelColumn.setCellValueFactory(data -> data.getValue().levelProperty());

        TableColumn<LogEntry, String> loggerColumn = new TableColumn<>("Logger");
        loggerColumn.setCellValueFactory(data -> data.getValue().loggerProperty());

        TableColumn<LogEntry, String> messageColumn = new TableColumn<>("Mensaje");
        messageColumn.setCellValueFactory(data -> data.getValue().messageProperty());

        tableView.getColumns().addAll(timeColumn, levelColumn, loggerColumn, messageColumn);
        return tableView;
    }

    private void loadEntries() {
        if (!Files.exists(ERROR_LOG_PATH)) {
            entries.clear();
            statusMessage.set("No se encontró el archivo de advertencias/errores aún.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(ERROR_LOG_PATH, StandardCharsets.UTF_8);
            ObservableList<LogEntry> refreshed = FXCollections.observableArrayList();
            for (String line : lines) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                String formattedTimestamp = matcher.group("timestamp");
                LocalDateTime timestamp = LocalDateTime.parse(formattedTimestamp, INPUT_FORMATTER);
                String displayTimestamp = timestamp.format(DISPLAY_FORMATTER);

                String level = matcher.group("level").trim();
                String logger = matcher.group("logger").trim();
                String message = matcher.group("message").trim();

                refreshed.add(new LogEntry(displayTimestamp, level, logger, message));
            }
            entries.setAll(refreshed);
            statusMessage.set(refreshed.isEmpty()
                    ? "No hay advertencias ni errores registrados en el archivo dedicado."
                    : "Mostrando " + refreshed.size() + " advertencias/errores registrados.");
        } catch (IOException e) {
            entries.clear();
            statusMessage.set("No se pudo leer el archivo de errores (" + e.getMessage() + ").");
        }
    }

    private void watchForChanges() {
        try {
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changed = (Path) event.context();
                        if (changed != null && changed.getFileName().equals(ERROR_LOG_PATH.getFileName())) {
                            Platform.runLater(this::loadEntries);
                        }
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException | java.nio.file.ClosedWatchServiceException ignored) {
            // The watcher is stopped when the application closes.
        }
    }

    public static void main(String[] args) {
        LoggingConfig.initLogging();
        launch(args);
    }

    private static final class LogEntry {
        private final StringProperty timestamp = new SimpleStringProperty();
        private final StringProperty level = new SimpleStringProperty();
        private final StringProperty logger = new SimpleStringProperty();
        private final StringProperty message = new SimpleStringProperty();

        private LogEntry(String timestamp, String level, String logger, String message) {
            this.timestamp.set(timestamp);
            this.level.set(level);
            this.logger.set(logger);
            this.message.set(message);
        }

        StringProperty timestampProperty() {
            return timestamp;
        }

        StringProperty levelProperty() {
            return level;
        }

        StringProperty loggerProperty() {
            return logger;
        }

        StringProperty messageProperty() {
            return message;
        }
    }
}