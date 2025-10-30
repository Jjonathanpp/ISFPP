package colectivo.logica;

import colectivo.aplicacion.Coordinador;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.apache.log4j.Logger;


import java.util.List;

public class Empresa {

    private static Empresa empresa = null;
    private Coordinador coordinador;
    private static final Logger LOGGER = Logger.getLogger(Empresa.class);
    private String nombre;
    private List<Linea> lineas;
    private List<Parada> paradas;
    private List<Tramo> tramos;

    public static Empresa getEmpresa(){
        if(empresa == null){
            empresa = new Empresa();
        }
        return empresa;
    }

    private Empresa(){
        super();
        lineas = null;
        paradas = null;
        tramos = null;
    }

    public void agregarLinea(Linea linea){
        if(lineas.contains(linea)) {
            LOGGER.warn("La línea ya existe: " + linea.getCodigo());
            return;
        }
        lineas.add(linea);
        LOGGER.info("Línea agregada correctamente: " + linea.getCodigo());
    }
    public void modificarLinea(Linea linea){
        int index = lineas.indexOf(linea);
        lineas.set(index, linea);
        LOGGER.info("Línea modificada correctamente: " + linea.getCodigo());
    }
    public void eliminarLinea(Linea linea) throws Exception {
        for(Parada l : paradas)
            if (l.getLineas().equals(linea)) {
                LOGGER.error("No se puede eliminar la línea " + linea.getCodigo() + " porque tiene paradas asociadas.");
                throw new Exception("La línea tiene paradas asociadas"); //CAMBIAR A UNA EXCEPCION CREADA POR NOSOTROS
            }
        Linea l = buscarLinea(linea);
        lineas.remove(l);
        LOGGER.info("Línea eliminada correctamente: " + linea.getCodigo());
    }

    public Linea buscarLinea(Linea linea){
        int pos = lineas.indexOf(linea);
        if(pos == -1) {
            LOGGER.warn("No se encontro la linea: " + linea.getCodigo());
            return null;
        }
        return lineas.get(pos);
    }

    //-----------------------------

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public List<Linea> getLineas() {
        return lineas;
    }
    public List<Parada> getParadas() {
        return paradas;
    }
    public List<Tramo> getTramos() {
        return tramos;
    }
    public void setCoordinador(Coordinador coordinador) {
        this.coordinador = coordinador;
    }

}
