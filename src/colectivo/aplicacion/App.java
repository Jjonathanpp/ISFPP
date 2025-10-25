package colectivo.aplicacion;
import colectivo.interfaz.Controlador;
import colectivo.interfaz.InterfazView;
import colectivo.util.LoggingConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        InterfazView view = new InterfazView();
        Controlador controlador = new Controlador(view);
        controlador.init();

        stage.setTitle("ISFPP - Interfaz de Recorridos");
        stage.setScene(new Scene(view.getRoot(), 520, 600));
        stage.show();
    }

    public static void main(String[] args) {
        LoggingConfig.initLogging();
        launch(args);
    }
}
