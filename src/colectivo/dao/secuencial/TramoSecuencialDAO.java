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
    public void insertar(Tramo tramo) throws InstanciaNoExisteEnBDException, InstanciaExisteEnBDException {
        // Verifico que existan ambas paradas
        if (!existeParada(tramo.getInicio().getCodigo()) || !existeParada(tramo.getFin().getCodigo())) {
            throw new InstanciaNoExisteEnBDException("Una o ambas paradas del tramo no existen en la base de datos.");
        }
        // Verifico que el tramo no exista ya
        if (existeTramo(tramo.getInicio().getCodigo(), tramo.getFin().getCodigo(), tramo.getTipo())) {
            throw new InstanciaExisteEnBDException("El tramo ya existe en la base de datos.");
        }
        String sql = "INSERT INTO tramo (inicio, destino, tiempo, tipo) VALUES (?, ?, ?, ?)";
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
        String sql = "UPDATE tramo SET tiempo = ?, tipo = ? WHERE inicio = (SELECT codigo FROM parada WHERE codigo = ?) AND destino = (SELECT codigo FROM parada WHERE codigo = ?)";
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
    public void borrar(Tramo tramo) throws InstanciaNoExisteEnBDException {
        // Verifico que el tramo no exista ya
        if (!existeTramo(tramo.getInicio().getCodigo(), tramo.getFin().getCodigo(), tramo.getTipo())) {
            throw new InstanciaNoExisteEnBDException("El tramo no existe en la base de datos.");
        }
        String sql = "DELETE FROM tramo WHERE inicio = (SELECT codigo FROM parada WHERE codigo = ?) AND destino = (SELECT codigo FROM parada WHERE codigo = ?)";
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

        String sql = "SELECT t.inicio, t.destino, t.tiempo, t.tipo " +
                "FROM tramo t";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int codigoInicio = rs.getInt("inicio");
                    int codigoDestino = rs.getInt("destino");
                    int tiempo = rs.getInt("tiempo");
                    int tipo = rs.getInt("tipo");
                    Parada inicio = paradas.get(codigoInicio);
                    Parada destino = paradas.get(codigoDestino);
                    if (inicio != null && destino != null) {
                        Tramo t = new Tramo(tiempo, tipo, inicio, destino);
                        resultado.put(codigoInicio + "-" + codigoDestino, t);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }

    // Verifica si una parada existe
    private boolean existeParada(String codigoParada) {
        String sql = "SELECT 1 FROM parada WHERE codigo = ?";
        try (Connection conn = Conexion.getInstancia().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoParada);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Verifica si el tramo ya existe
    private boolean existeTramo(String inicio, String destino, int tipo) {
        String sql = "SELECT 1 FROM tramo WHERE inicio = ? AND destino = ? AND tipo = ?";
        try (Connection conn = Conexion.getInstancia().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inicio);
            ps.setString(2, destino);
            ps.setInt(3, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
