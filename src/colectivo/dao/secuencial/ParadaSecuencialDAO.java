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

    @Override
    public void insertar(Parada parada) {
        String sql = "INSERT INTO parada (id, direccion, latitud, longitud) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getCodigo());
                ps.setString(2, parada.getDireccion());
                ps.setDouble(3, parada.getLatitud());
                ps.setDouble(4, parada.getLongitud());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actualizar(Parada parada) {
        String sql = "UPDATE parada SET direccion = ?, latitud = ?, longitud = ? WHERE id = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getDireccion());
                ps.setDouble(2, parada.getLatitud());
                ps.setDouble(3, parada.getLongitud());
                ps.setString(4, parada.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void borrar(Parada parada) {
        String sql = "DELETE FROM parada WHERE id = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        Map<Integer, Parada> resultado = new HashMap<>();
        String sql = "SELECT id, direccion, latitud, longitud FROM parada";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String codigo = rs.getString("id");
                    String direccion = rs.getString("direccion");
                    double latitud = rs.getDouble("latitud");
                    double longitud = rs.getDouble("longitud");
                    Parada p = new Parada(codigo, direccion, latitud, longitud);
                    resultado.put(Integer.parseInt(codigo), p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }

}
