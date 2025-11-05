package colectivo.aplicacion;
import colectivo.interfaz.Controler;

import colectivo.logica.Empresa;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1) Crear coordinador y cargar datos
            Coordinador coordinador = new Coordinador();
            coordinador.setEmpresa(Empresa.getEmpresa()); //Porque usa singleton
            coordinador.cargarDatos();

            // 2) Hacer que el coordinador construya (arme) la vista
            coordinador.construirVista();
            VistaInterfaz vista = coordinador.getVista();

            // 3) Preparar la lógica desacoplada
            RecorridoDesacoplador recorridoService = coordinador.getRecorridoDesacoplador();

            // 4) Crear y configurar el controlador
            Controler controlador = new Controler(vista, recorridoService, coordinador);
            controlador.inicializar();

            // 5) App gestiona la ventana
            stage.setTitle("ISFPP - Interfaz de Recorridos");
            stage.setScene(new Scene(vista.getRoot(), 520, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            //Creo que va el logger aca
        }

    }

    public static void main(String[] args) { launch(args); }
}
