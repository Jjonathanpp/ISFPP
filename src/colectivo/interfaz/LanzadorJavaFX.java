package colectivo.interfaz;

import colectivo.aplicacion.AplicacionPrincipal;
import colectivo.aplicacion.Coordinador;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class LanzadorJavaFX extends Application {

    private static final Logger LOGGER = Logger.getLogger(LanzadorJavaFX.class);

    @Override
    public void start(Stage stage) {
        try {

            AplicacionPrincipal app = new AplicacionPrincipal();


            app.iniciar();


            Coordinador coordinador = app.getCoordinador();
            VistaInterfaz vista = coordinador.getVista();


            stage.setTitle("Planificador Urbano");
            stage.setScene(new Scene(vista.getRoot(), 900, 720));
            stage.show();

        } catch (Exception e) {
            LOGGER.error("Error al iniciar JavaFX", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
