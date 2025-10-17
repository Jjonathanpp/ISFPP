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
        String sql = "INSERT INTO linea (id, nombre) VALUES (?, ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        insertarLineaParada(linea);
    }

    @Override
    public void actualizar(Linea linea) {
        String sql = "UPDATE linea SET nombre = ? WHERE id = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getNombre());
                ps.setString(2, linea.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        actualizarLineaParada(linea);
    }

    @Override
    public void borrar(Linea linea) {
        String sql = "DELETE FROM linea WHERE id = ?";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        borrarLineaParada(linea);
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        Map<String, Linea> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sqlLineas = "SELECT id, nombre FROM linea";
        String sqlParadasLinea = "SELECT l.id as cod_linea, p.id as cod_parada " +
                "FROM linea_parada pl " +
                "JOIN linea l ON pl.id_linea = l.id " +
                "JOIN parada p ON pl.id_parada = p.id " +
                "ORDER BY pl.id_linea, pl.orden";

        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement psLineas = conn.prepareStatement(sqlLineas);
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
                    String codigo = rsLineas.getString("id");
                    String nombre = rsLineas.getString("nombre");
                    List<Parada> paradasLinea = lineasParadas.getOrDefault(codigo, new ArrayList<>());
                    if (paradasLinea.size() >= 2) {
                        Linea l = new Linea(codigo, nombre, paradasLinea);
                        resultado.put(codigo, l);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }

    //================================= DAO DE LineaParada =================================
    private void insertarLineaParada(Linea linea) {
        String sql = "INSERT INTO linea_parada (id_linea, id_parada, orden) VALUES (?, ?, ?)";
        List<Parada> paradas = linea.getParadas();
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            for (int i = 0; i < paradas.size(); i++) {
                Parada parada = paradas.get(i);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, linea.getCodigo());
                    ps.setString(2, parada.getCodigo());
                    ps.setInt(3, i + 1); // orden, comienza en 1
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void actualizarLineaParada(Linea linea){
        borrarLineaParada(linea);
        insertarLineaParada(linea);

    }
    private void borrarLineaParada(Linea linea){
        String sql = "DELETE FROM linea_parada WHERE id_linea = (SELECT id FROM linea WHERE id = ?)";
        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
