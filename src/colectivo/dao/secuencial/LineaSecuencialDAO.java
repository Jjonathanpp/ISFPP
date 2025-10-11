package colectivo.dao.secuencial;

import colectivo.conexion.Conexion;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineaSecuencialDAO implements LineaDAO {

    @Override
    public void insertar(Linea linea) {
        String sql = "INSERT INTO linea (codigo, nombre) VALUES (?, ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, linea.getCodigo());
            ps.setString(2, linea.getNombre());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Para la tabla paradaslineas deberías también insertar los pares (id_linea, id_parada, orden) aquí
    }

    @Override
    public void actualizar(Linea linea) {
        String sql = "UPDATE linea SET nombre = ? WHERE codigo = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, linea.getNombre());
            ps.setString(2, linea.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void borrar(Linea linea) {
        String sql = "DELETE FROM linea WHERE codigo = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, linea.getCodigo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        Map<String, Linea> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sqlLineas = "SELECT codigo, nombre FROM linea";
        String sqlParadasLinea = "SELECT l.codigo as cod_linea, p.codigo as cod_parada " +
                "FROM paradaslineas pl " +
                "JOIN linea l ON pl.id_linea = l.id " +
                "JOIN parada p ON pl.id_parada = p.id " +
                "ORDER BY pl.id_linea, pl.orden";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement psLineas = conn.prepareStatement(sqlLineas);
             ResultSet rsLineas = psLineas.executeQuery()) {

            Map<String, List<Parada>> lineasParadas = new HashMap<>();

            // Primero armamos el mapa de paradas para cada línea
            try (PreparedStatement psPL = conn.prepareStatement(sqlParadasLinea);
                 ResultSet rsPL = psPL.executeQuery()) {
                while (rsPL.next()) {
                    String codLinea = rsPL.getString("cod_linea");
                    String codParada = rsPL.getString("cod_parada");
                    Parada parada = paradas.get(Integer.parseInt(codParada));
                    if (parada != null) {
                        lineasParadas.computeIfAbsent(codLinea, k -> new ArrayList<>()).add(parada);
                    }
                }
            }

            // Ahora armamos los objetos Linea con la lista de paradas
            while (rsLineas.next()) {
                String codigo = rsLineas.getString("codigo");
                String nombre = rsLineas.getString("nombre");
                List<Parada> paradasLinea = lineasParadas.getOrDefault(codigo, new ArrayList<>());
                if (paradasLinea.size() >= 2) {
                    // NOTA: el constructor que tienes en Linea requiere dos paradas mínimas
                    Linea l = new Linea(codigo, nombre, paradasLinea);
                    resultado.put(codigo, l);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }
}
