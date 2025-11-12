package colectivo.aplicacion;

import colectivo.interfaz.Controler;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Calculo;
import colectivo.logica.Empresa;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.time.LocalTime;
import java.util.Map;

public class Coordinador {
    private static final Logger LOGGER = Logger.getLogger(Coordinador.class);
    private VistaInterfaz vista = new VistaInterfaz();
    private Controler controlerMVC = new Controler(vista, this);
    private Empresa empresa;

    public void construirVistaConControler() {
        getControlerMVC().construirVista();
    }
    public VistaInterfaz getVista() {
        return vista;
    }
    public Controler getControlerMVC() {
        return controlerMVC;
    }
    public void desacopladorLogica(Parada origen, Parada destino, int dia, LocalTime hora) {
        LOGGER.debug("Invocando buscarRecorridos con parámetros: dia=" + dia + ", hora=" + hora);
        getControlerMVC().buscarYMostrar(Calculo.calcularRecorrido(origen, destino, dia, hora, empresa.getTramos()),
                origen, destino, hora);
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Map<Integer, Parada> listarParadas() {
        return empresa.getParadas();
    }
}
