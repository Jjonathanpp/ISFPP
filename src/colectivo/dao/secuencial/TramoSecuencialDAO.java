package colectivo.dao.secuencial;

import colectivo.conexion.Conexion;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
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
    public void borrar(Tramo tramo) {

    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        //Creamos el mapa a devolver
        Map<String, Tramo> resultado = new HashMap<>();

        // Primero cargamos todas las paradas
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sql = "SELECT parada_inicio, parada_fin, tiempo_recorrido, tipo_recorrido FROM isfpp.tramo";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int codInicio = rs.getInt("parada_inicio");
                int codFin = rs.getInt("parada_fin");
                int tiempo = rs.getInt("tiempo_recorrido");
                int tipo = rs.getInt("tipo_recorrido");

                Parada inicio = paradas.get(codInicio);
                Parada fin = paradas.get(codFin);

                if (inicio != null && fin != null) {
                    Tramo t = new Tramo(tiempo, tipo, inicio, fin);
                    String clave = inicio.getCodigo() + "-" + fin.getCodigo();
                    resultado.put(clave, t);
                } else {
                    System.out.println("Parada no encontrada para tramo: " + codInicio + " → " + codFin);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resultado;
    }

}
