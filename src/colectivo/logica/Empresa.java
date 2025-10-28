package colectivo.logica;

import colectivo.aplicacion.Coordinador;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

import java.util.List;

public class Empresa {

    private static Empresa empresa = null;
    private Coordinador coordinador;
    //Logger
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
        if(lineas.contains(linea))
            return;
        lineas.add(linea);
        //Logger
    }
    public void modificarLinea(Linea linea){
        int index = lineas.indexOf(linea);
        lineas.set(index, linea);
        //Logger
    }
    public void eliminarLinea(Linea linea) throws Exception {
        for(Parada l : paradas)
            if (l.getLineas().equals(linea))
                throw new Exception("La línea tiene paradas asociadas"); //CAMBIAR A UNA EXCEPCION CREADA POR NOSOTROS
        Linea l = buscarLinea(linea);
        lineas.remove(l);
        //Logger
    }

    public Linea buscarLinea(Linea linea){
        int pos = lineas.indexOf(linea);
        if(pos == -1)
            return null;
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
