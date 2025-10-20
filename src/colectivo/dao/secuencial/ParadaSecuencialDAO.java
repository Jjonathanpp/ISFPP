package colectivo.dao.secuencial;

import colectivo.conexion.Conexion;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import java.util.Map;
import colectivo.modelo.Parada;
import java.sql.*;
import java.util.*;

public class ParadaSecuencialDAO implements ParadaDAO {




    @Override
    public void insertar(Parada parada) throws InstanciaExisteEnBDException {

    }

    @Override
    public void actualizar(Parada parada) {

    }

    @Override
    public void borrar(Parada parada) throws InstanciaNoExisteEnBDException {

    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        return new HashMap<>();
    }

    // Verifica si ya existe la parada con ese código antes de insertar
    private boolean existe(String codigo) {
        return false;
    }

}
