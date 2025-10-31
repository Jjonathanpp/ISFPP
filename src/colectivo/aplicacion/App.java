package colectivo.aplicacion;

import colectivo.interfaz.Controler;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Empresa;

public class App {

    private final Coordinador coordinador;
    private final VistaInterfaz vista;
    private final Controler controlador;
    private final Configuracion configuracion;

    public App() {
        coordinador = new Coordinador();
        coordinador.setEmpresa(Empresa.getEmpresa());

        vista = coordinador.getVista();
        controlador = coordinador.getControlerMVC();

        coordinador.construirVistaConControler();
        controlador.inicializar();

        configuracion = Configuracion.getInstance();
    }

    public VistaInterfaz getVista() {
        return vista;


    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }
}
