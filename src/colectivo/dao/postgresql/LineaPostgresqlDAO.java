package colectivo.dao.postgresql;


import colectivo.conexion.Conexion;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Frecuencia;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineaPostgresqlDAO implements LineaDAO {
    @Override
    public void insertar(Linea linea) throws InstanciaExisteEnBDException {
        if (existe(linea.getCodigo())) {
            throw new InstanciaExisteEnBDException("La línea con código " + linea.getCodigo() + " ya existe en la base de datos.");
        }

        String sql = "INSERT INTO linea (codigo, nombre) VALUES (?, ?)";
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
        String sql = "UPDATE linea SET nombre = ? WHERE codigo = ?";
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
    public void borrar(Linea linea) throws InstanciaNoExisteEnBDException {
        if (!existe(linea.getCodigo())) {
            throw new InstanciaNoExisteEnBDException("La línea con código " + linea.getCodigo() + " no existe en la base de datos.");
        }
        borrarLineaParada(linea);
        String sql = "DELETE FROM linea WHERE codigo = ?";
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

    @Override
    public Map<String, Linea> buscarTodos() {
        Map<String, Linea> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sqlLineas = "SELECT codigo, nombre FROM linea";
        String sqlParadasLinea = "SELECT l.codigo as cod_linea, p.codigo as cod_parada " +
                "FROM linea_parada pl " +
                "JOIN linea l ON pl.codigo_linea = l.codigo " +
                "JOIN parada p ON pl.codigo_parada = p.codigo " +
                "ORDER BY pl.codigo_linea, pl.orden";
        String sqlFrecuencias = "Select codigo_linea, diasemana,hora from frecuencia";

        try {
            Connection conn = Conexion.getInstancia().getConnection();
            try (PreparedStatement psLineas = conn.prepareStatement(sqlLineas);
                 ResultSet rsLineas = psLineas.executeQuery()) {

                Map<String, List<Parada>> lineasParadas = new HashMap<>();
                Map<String, List<FrecuenciaData>> frecuenciasPorLinea = new HashMap<>();

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

                // Luego cargamos las frecuencias para cada línea
                try (PreparedStatement psFreq = conn.prepareStatement(sqlFrecuencias);
                     ResultSet rsFreq = psFreq.executeQuery()) {
                    while (rsFreq.next()) {
                        String codLinea = rsFreq.getString("codigo_linea");
                        int diaSemana = rsFreq.getInt("diasemana");
                        Time hora = rsFreq.getTime("hora");
                        if (hora != null) {
                            frecuenciasPorLinea
                                    .computeIfAbsent(codLinea, k -> new ArrayList<>())
                                    .add(new FrecuenciaData(diaSemana, hora.toLocalTime()));
                        }
                    }
                }

                // Ahora armamos los objetos Linea con la lista de paradas
                while (rsLineas.next()) {
                    String codigo = rsLineas.getString("codigo");
                    String nombre = rsLineas.getString("nombre");
                    List<Parada> paradasLinea = lineasParadas.getOrDefault(codigo, new ArrayList<>());
                    if (paradasLinea.size() >= 2) {
                        Linea l = new Linea(codigo, nombre, paradasLinea);
                        List<FrecuenciaData> datosFrecuencias = frecuenciasPorLinea.get(codigo);
                        if (datosFrecuencias != null) {
                            for (FrecuenciaData datos : datosFrecuencias) {
                                l.agregarFrecuencia(new Frecuencia(l, datos.diaSemana(), datos.hora()));
                            }
                        }
                        resultado.put(codigo, l);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultado;
    }

    // Método auxiliar para verificar existencia
    private boolean existe(String codigo) {
        String sql = "SELECT 1 FROM linea WHERE codigo = ?";
        try (Connection conn = Conexion.getInstancia().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Si hay algún resultado, existe
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    //================================= DAO DE LineaParada =================================
    private void insertarLineaParada(Linea linea) {
        String sql = "INSERT INTO linea_parada (codigo_linea, codigo_parada, orden) VALUES (?, ?, ?)";
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
        String sql = "DELETE FROM linea_parada WHERE codigo_linea = ?";
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
    private record FrecuenciaData(int diaSemana, LocalTime hora) {
    }

}
