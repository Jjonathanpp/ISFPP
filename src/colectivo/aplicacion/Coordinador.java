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
