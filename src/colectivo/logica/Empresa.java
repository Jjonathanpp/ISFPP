package colectivo.logica;

import colectivo.aplicacion.Coordinador;
import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.apache.log4j.Logger;


import java.util.List;
import java.util.Map;

public class Empresa {

    private static Empresa empresa = null;
    private Coordinador coordinador;
    private static final Logger LOGGER = Logger.getLogger(Empresa.class);
    private String nombre;
    //DAO
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;

    public static Empresa getEmpresa(){
        if(empresa == null){
            empresa = new Empresa();
        }
        return empresa;
    }

    private Empresa(){
        super();
        paradas = ((ParadaDAO) Factory.getInstancia("PARADA")).buscarTodos();
        tramos  = ((TramoDAO)  Factory.getInstancia("TRAMO")).buscarTodos();
        lineas  = ((LineaDAO)  Factory.getInstancia("LINEA")).buscarTodos();
    }

    public void agregarLinea(Linea linea){
        if(lineas.get(linea.getCodigo()) != null) {
            LOGGER.warn("La línea ya existe: " + linea.getCodigo());
            return;
        }
        lineas.put(linea.getCodigo(), linea);
        LOGGER.info("Línea agregada correctamente: " + linea.getCodigo());
    }
    public void modificarLinea(Linea linea){
        if(lineas.get(linea.getCodigo()) == null) {
            LOGGER.warn("No se puede modificar la línea porque no existe: " + linea.getCodigo());
            return;
        }
        lineas.replace(linea.getCodigo(), linea);
        LOGGER.info("Línea modificada correctamente: " + linea.getCodigo());
    }
    public void eliminarLinea(Linea linea) throws Exception {
        if(lineas.get(linea.getCodigo()) == null) {
            LOGGER.warn("No se puede eliminar la línea porque no existe: " + linea.getCodigo());
            return;
        }
        lineas.remove(linea.getCodigo());
        LOGGER.info("Línea eliminada correctamente: " + linea.getCodigo());
    }

    public Linea buscarLinea(Linea linea){
        if (lineas.get(linea) != null) {
            return lineas.get(linea.getCodigo());
        }
        LOGGER.warn("La línea a buscar es nula.");
        return null;
    }

    //-----------------------------

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
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
    public void setCoordinador(Coordinador coordinador) {
        this.coordinador = coordinador;
    }

}
