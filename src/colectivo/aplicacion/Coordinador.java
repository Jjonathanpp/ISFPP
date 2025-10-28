package colectivo.aplicacion;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Empresa;
import colectivo.logica.RecorridoDesacoplador;
import colectivo.logica.RecorridoDesacopladorImpl;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Coordinador {
    //==================================ATRIBUTOS==================================
    //Interfaz
    VistaInterfaz vista = new VistaInterfaz();
    //Logica
    RecorridoDesacoplador recorrido = new RecorridoDesacopladorImpl();
    //DAO
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;
    //Empresa
    private Empresa empresa;


    //==================================METODOS==================================
    //Interfaz
        //Construir vista
    public void construirVista() {
        vista.construirVista();
    }
    public VistaInterfaz getVista() {
        return vista;
    }
    //Construir el desacoplador de la logica
    public RecorridoDesacoplador getRecorridoDesacoplador() {
        return recorrido;
    }

    //Cargar los datos de DAO

    public void cargarDatos(){
        paradas = ((ParadaDAO) Factory.getInstancia("PARADA")).buscarTodos();
        tramos  = ((TramoDAO)  Factory.getInstancia("TRAMO")).buscarTodos();
        lineas  = ((LineaDAO)  Factory.getInstancia("LINEA")).buscarTodos();
    }
    //Recuperar los datos de DAO

    public Map<String, Tramo> getTramos() {
        return tramos;
    }
    public Map<Integer, Parada> getParadas() {
        return paradas;
    }
    public Map<String, Linea> getLineas() {
        return lineas;
    }
    //Cosas de empresa ¿¿

    public Empresa getEmpresa() {
        return empresa;
    }
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
    public List<Linea> listarLineas() {
        return empresa.getLineas();
    }
    public List<Parada> listarParadas() {
        // 1) si empresa tiene lista poblada, devuelvela
        if (empresa != null && empresa.getParadas() != null) {
            return empresa.getParadas();
        }
        // 2) si coordinador cargó el mapa de paradas, conviértelo a lista
        if (paradas != null && !paradas.isEmpty()) {
            return new ArrayList<>(paradas.values());
        }
        // 3) fallback vacío para evitar NPE
        return new ArrayList<>();
    }

    public List<Tramo> listarTramos() {
        return empresa.getTramos();
    }
    public Linea buscarLinea(Linea linea){
        return empresa.buscarLinea(linea);
    }

}
