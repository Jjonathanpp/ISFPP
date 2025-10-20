package colectivo.dao.secuencial;

import colectivo.conexion.Conexion;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TramoSecuencialDAO implements TramoDAO {

    @Override
    public void insertar(Tramo tramo) {

    }

    @Override
    public void actualizar(Tramo tramo) {

    }

    @Override
    public void borrar(Tramo tramo) throws InstanciaNoExisteEnBDException {

    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        return new HashMap<>();
    }

    // Verifica si una parada existe
    private boolean existeParada(String codigoParada) {
        return false;
    }

    // Verifica si el tramo ya existe
    private boolean existeTramo(String inicio, String destino, int tipo) {
        return false;
    }
}
