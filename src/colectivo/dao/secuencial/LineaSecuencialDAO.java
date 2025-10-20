package colectivo.dao.secuencial;



import colectivo.conexion.Conexion;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.*;

import java.io.FileNotFoundException;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;

public class LineaSecuencialDAO implements LineaDAO {

    private List<Linea> list;
    private String name;
    private boolean actualizar;

    public LineaSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("secuencial");
        name = rb.getString("linea");
        actualizar = true;
    }

    private List<Linea> readFromFile(String file) throws FileNotFoundException {
        return new ArrayList<>();
    }

    private void writeToFile(List<Linea> list, String file) {

    }

    @Override
    public void insertar(Linea linea) throws InstanciaExisteEnBDException {

    }

    @Override
    public void actualizar(Linea linea) {

    }

    @Override
    public void borrar(Linea linea) throws InstanciaNoExisteEnBDException {

    }

    @Override
    public Map<String, Linea> buscarTodos() {
        return new HashMap<>();
    }

    // Método auxiliar para verificar existencia
    private boolean existe(String codigo) {
        return false;
    }
    //================================= DAO DE LineaParada =================================
    private void insertarLineaParada(Linea linea) {
    }

    private void actualizarLineaParada(Linea linea){

    }
    private void borrarLineaParada(Linea linea){

    }
    private record FrecuenciaData(int diaSemana, LocalTime hora) {
    }

}
