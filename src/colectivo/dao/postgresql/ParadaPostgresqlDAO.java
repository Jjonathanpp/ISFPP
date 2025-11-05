package colectivo.dao.postgresql;

import colectivo.conexion.Conexion;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ParadaPostgresqlDAO implements ParadaDAO {

    private static final Logger LOGGER = Logger.getLogger(ParadaPostgresqlDAO.class);

    @Override
    public void insertar(Parada parada) throws InstanciaExisteException {
        if (existe(parada.getCodigo())) {
            throw new InstanciaExisteException("La parada con código " + parada.getCodigo() + " ya existe en la base de datos.");
        }
        String sql = "INSERT INTO parada (codigo, direccion, latitud, longitud) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getCodigo());
                ps.setString(2, parada.getDireccion());
                ps.setDouble(3, parada.getLatitud());
                ps.setDouble(4, parada.getLongitud());
                ps.executeUpdate();
                LOGGER.info("Parada insertada en PostgreSQL: " + parada.getCodigo());
            }
        } catch (SQLException e) {
            LOGGER.error("Error al insertar la parada " + parada.getCodigo(), e);
        }
    }

    @Override
    public void actualizar(Parada parada) {
        String sql = "UPDATE parada SET direccion = ?, latitud = ?, longitud = ? WHERE codigo = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getDireccion());
                ps.setDouble(2, parada.getLatitud());
                ps.setDouble(3, parada.getLongitud());
                ps.setString(4, parada.getCodigo());
                ps.executeUpdate();
                LOGGER.info("Parada actualizada en PostgreSQL: " + parada.getCodigo());
            }
        } catch (SQLException e) {
            LOGGER.error("Error al actualizar la parada " + parada.getCodigo(), e);
        }
    }

    @Override
    public void borrar(Parada parada) throws InstanciaNoExisteException {
        if (!existe(parada.getCodigo())) {
            throw new InstanciaNoExisteException("La parada con código " + parada.getCodigo() + " no existe en la base de datos.");
        }
        String sql = "DELETE FROM parada WHERE codigo = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parada.getCodigo());
                ps.executeUpdate();
                LOGGER.info("Parada borrada en PostgreSQL: " + parada.getCodigo());
            }
        } catch (SQLException e) {
            LOGGER.error("Error al borrar la parada " + parada.getCodigo(), e);
        }
    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        Map<Integer, Parada> resultado = new HashMap<>();
        String sql = "SELECT codigo, direccion, latitud, longitud FROM parada";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String codigo = rs.getString("codigo");
                    String direccion = rs.getString("direccion");
                    double latitud = rs.getDouble("latitud");
                    double longitud = rs.getDouble("longitud");
                    Parada p = new Parada(codigo, direccion, latitud, longitud);
                    resultado.put(Integer.parseInt(codigo), p);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error al buscar todas las paradas", e);
        }
        return resultado;
    }

    private boolean existe(String codigo) {
        String sql = "SELECT 1 FROM parada WHERE codigo = ?";
        try (Connection conn = Conexion.getInstancia().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Si hay resultado, existe
            }
        } catch (SQLException e) {
            LOGGER.error("Error al verificar existencia de la parada " + codigo, e);
        }
        return false;
    }

}
