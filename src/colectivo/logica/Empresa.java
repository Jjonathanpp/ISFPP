package colectivo.logica;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;


import java.util.Map;

public class Empresa {

    private static Empresa empresa = null;
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;
    private final ParadaDAO paradaDAO;
    private final LineaDAO lineaDAO;
    private final TramoDAO tramoDAO;

    public static Empresa getEmpresa(){
        if(empresa == null){
            empresa = new Empresa();
        }
        return empresa;
    }

    private Empresa(){
        super();
        this.paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
        this.lineaDAO  = (LineaDAO)  Factory.getInstancia("LINEA");
        this.tramoDAO  = (TramoDAO)  Factory.getInstancia("TRAMO");

        paradas = paradaDAO.buscarTodos();
        lineas  = lineaDAO.buscarTodos();
        tramos  = tramoDAO.buscarTodos();
    }

    public Map<String, Tramo> getTramos() {
        return tramos;
    }
    public Map<Integer, Parada> getParadas() {
        return paradas;
    }
    public Map<String, Linea> getLineas() {
        return lineas;
    }


}
