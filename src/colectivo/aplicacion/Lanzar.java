package colectivo.aplicacion;

import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.interfaz.VistaInterfaz;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class Lanzar extends Application {

    private static final Logger LOGGER = Logger.getLogger(Lanzar.class);

    private App app;

    @Override
    public void start(Stage stage) {
        try {
            app = new App();
            VistaInterfaz vista = app.getVista();
            Configuracion configuracion = app.getConfiguracion();

            stage.setTitle(configuracion.getBundle().getString("app.windowTitle"));
            configuracion.localeProperty().addListener((obs, oldLocale, newLocale) ->
                    stage.setTitle(configuracion.getBundle().getString("app.windowTitle"))
            );
            stage.setScene(new Scene(vista.getRoot(), 900, 720));
            stage.show();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("ShutdownHook: cerrando conexion BD...");
            try {
                colectivo.conexion.Conexion.cerrar();
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
            }
        }));
        launch(args);
    }
}