package colectivo.aplicacion;

import colectivo.interfaz.Controler;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Empresa;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1) Crear coordinador y cargar datos
            Coordinador coordinador = new Coordinador();
            coordinador.setEmpresa(Empresa.getEmpresa()); //Porque usa singleton

            // 2) Hacer que el coordinador construya (arme) la vista
            VistaInterfaz vista = coordinador.getVista(); //Aca es donde se crea la vista unicamente para pasarsela al controlador
            Controler controlador = new Controler(vista, coordinador);

            // 4) Crear y configurar el controlador
            coordinador.construirVistaConControler();
            controlador.inicializar();

            // 5) App gestiona la ventana
            Configuracion configuracion = Configuracion.getInstance();
            ResourceBundle bundle = configuracion.getBundle();
            stage.setTitle(bundle.getString("app.windowTitle"));
            configuracion.localeProperty().addListener((obs, oldLocale, newLocale) ->
                    stage.setTitle(configuracion.getBundle().getString("app.windowTitle"))
            );
            stage.setScene(new Scene(vista.getRoot(), 520, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            //Creo que va el logger aca
        }

    }

    public static void main(String[] args) {
        //ESTO se supone que es el shutdown hook para cerrar la conexion a la BD (no se si habria que hacer algo mas con esto)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("ShutdownHook: cerrando conexion BD...");
            try { colectivo.conexion.Conexion.cerrar(); } catch (Throwable t) { t.printStackTrace(); }
        }));
        launch(args);
        launch(args);
    }
}
