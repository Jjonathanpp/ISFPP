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
        String sql = "INSERT INTO tramo (id_parada_inicio, id_parada_fin, tiempo, tipo) VALUES ((SELECT id FROM parada WHERE codigo = ?), (SELECT id FROM parada WHERE codigo = ?), ?, ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tramo.getInicio().getCodigo());
            ps.setString(2, tramo.getFin().getCodigo());
            ps.setInt(3, tramo.getTiempo());
            ps.setInt(4, tramo.getTipo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actualizar(Tramo tramo) {
        // Aquí deberías identificar el tramo (por inicio y fin) y actualizar tiempo/tipo
        String sql = "UPDATE tramo SET tiempo = ?, tipo = ? WHERE id_parada_inicio = (SELECT id FROM parada WHERE codigo = ?) AND id_parada_fin = (SELECT id FROM parada WHERE codigo = ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tramo.getTiempo());
            ps.setInt(2, tramo.getTipo());
            ps.setString(3, tramo.getInicio().getCodigo());
            ps.setString(4, tramo.getFin().getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void borrar(Tramo tramo) {
        String sql = "DELETE FROM tramo WHERE id_parada_inicio = (SELECT id FROM parada WHERE codigo = ?) AND id_parada_fin = (SELECT id FROM parada WHERE codigo = ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tramo.getInicio().getCodigo());
            ps.setString(2, tramo.getFin().getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        Map<String, Tramo> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sql = "SELECT t.id, p1.codigo as cod_inicio, p2.codigo as cod_fin, t.tiempo, t.tipo " +
                "FROM tramo t " +
                "JOIN parada p1 ON t.id_parada_inicio = p1.id " +
                "JOIN parada p2 ON t.id_parada_fin = p2.id";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codInicio = rs.getString("cod_inicio");
                String codFin = rs.getString("cod_fin");
                int tiempo = rs.getInt("tiempo");
                int tipo = rs.getInt("tipo");
                Parada inicio = paradas.get(Integer.parseInt(codInicio));
                Parada fin = paradas.get(Integer.parseInt(codFin));
                if (inicio != null && fin != null) {
                    Tramo t = new Tramo(tiempo, tipo, inicio, fin);
                    resultado.put(codInicio + "-" + codFin, t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }

}
