package colectivo.aplicacion;

import colectivo.interfaz.Controler;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Calculo;
import colectivo.logica.Empresa;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.apache.log4j.Logger;

import java.time.LocalTime;
import java.util.Map;

public class Coordinador {
    //==================================ATRIBUTOS==================================
    //Logger
    private static final Logger LOGGER = Logger.getLogger(Coordinador.class);

    //Interfaz
    /*
    PREGUNTAR: la vista de la interfaz, tiene que hacerla el coordinador o el controlador? Tiene que haber un contacto del
    coordinador con la vista? actualmente solo la crea y no hace nada mas. Se usa unicamente en App para pasarla al controlador
     */
    VistaInterfaz vista = new VistaInterfaz();
    Controler controlerMVC = new Controler(vista, this);

    //Logica
    //Se crea un metodo directamente porque la clase Calculo contiene todas clases privadas y la unica publica es static

    //Empresa
    private Empresa empresa;


    //==================================METODOS==================================
    //Interfaz
        //Construir vista
    public void construirVistaConControler() {
        getControlerMVC().construirVista();
    }
    public VistaInterfaz getVista() {
        return vista;
    }
    public Controler getControlerMVC() {
        return controlerMVC;
    }
    //Construir el desacoplador de la logica
    public void desacopladorLogica(Parada origen, Parada destino, int dia, LocalTime hora) {
        LOGGER.debug("Invocando buscarRecorridos con parámetros: dia=" + dia + ", hora=" + hora);
        getControlerMVC().buscarYMostrar(Calculo.calcularRecorrido(origen, destino, dia, hora, empresa.getTramos()),
                origen, destino, hora);
    }


    //Cosas de empresa ¿¿

    public Empresa getEmpresa() {
        return empresa;
    }
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Map<Integer, Parada> listarParadas() {
        return empresa.getParadas();
    }

    public Map<String, Tramo> listarTramos() {
        return empresa.getTramos();
    }
    public Linea buscarLinea(Linea linea){
        return empresa.buscarLinea(linea);
    }

}
