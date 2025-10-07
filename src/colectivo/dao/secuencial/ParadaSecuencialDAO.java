package colectivo.dao.secuencial;

import colectivo.conexion.Conexion;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import java.util.Map;
import colectivo.modelo.Parada;
import java.sql.*;
import java.util.*;

public class ParadaSecuencialDAO implements ParadaDAO {


    private Linea l; //Borrar para que quede bien

    @Override
    public void insertar(Parada parada) {

    }

    @Override
    public void actualizar(Parada parada) {

    }

    @Override
    public void borrar(Parada parada) {

    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        Map<Integer, Parada> resultado = new HashMap<>();

        String sql = "SELECT cod_parada, nom_parada, latitud, longitud FROM isfpp.parada";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String codigoStr = rs.getString("cod_parada");
                int codigoInt = Integer.parseInt(codigoStr); // conversión segura si los datos son numéricos

                String direccion = rs.getString("nom_parada");
                double latitud = rs.getDouble("latitud");
                double longitud = rs.getDouble("longitud");

                Parada p = new Parada(
                        codigoStr, // sigue siendo String en el modelo
                        direccion,
                        latitud,
                        longitud,
                        l = new Linea("1", "Linea 1", null, null, null) // Línea ficticia por ahora
                );

                resultado.put(codigoInt, p);
            }

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }

        return resultado;
    }

}
