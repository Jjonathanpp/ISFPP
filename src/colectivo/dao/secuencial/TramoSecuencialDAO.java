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
        String sql = "INSERT INTO tramo (parada_origen, parada_destino, duracion_seg, tipo) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tramo.getInicio().getCodigo());
                ps.setString(2, tramo.getFin().getCodigo());
                ps.setInt(3, tramo.getTiempo());
                ps.setInt(4, tramo.getTipo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actualizar(Tramo tramo) {
        String sql = "UPDATE tramo SET duracion_seg = ?, tipo = ? WHERE parada_origen = (SELECT id FROM parada WHERE id = ?) AND parada_destino = (SELECT id FROM parada WHERE id = ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tramo.getTiempo());
                ps.setInt(2, tramo.getTipo());
                ps.setString(3, tramo.getInicio().getCodigo());
                ps.setString(4, tramo.getFin().getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void borrar(Tramo tramo) {
        String sql = "DELETE FROM tramo WHERE parada_origen = (SELECT id FROM parada WHERE id = ?) AND parada_destino = (SELECT id FROM parada WHERE id = ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tramo.getInicio().getCodigo());
                ps.setString(2, tramo.getFin().getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        Map<String, Tramo> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sql = "SELECT t.parada_origen, t.parada_destino, t.duracion_seg, t.tipo " +
                "FROM tramo t";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idInicio = rs.getInt("parada_origen");
                    int idFin = rs.getInt("parada_destino");
                    int tiempo = rs.getInt("duracion_seg");
                    int tipo = rs.getInt("tipo");
                    Parada inicio = paradas.get(idInicio);
                    Parada fin = paradas.get(idFin);
                    if (inicio != null && fin != null) {
                        Tramo t = new Tramo(tiempo, tipo, inicio, fin);
                        resultado.put(idInicio + "-" + idFin, t);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }
}
