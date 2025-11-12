package colectivo.dao.postgresql;


import colectivo.conexion.BDConexion;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Frecuencia;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineaPostgresqlDAO implements LineaDAO {

    private static final Logger LOGGER = Logger.getLogger(LineaPostgresqlDAO.class);
    private final Connection conn = BDConexion.getConnection();

    @Override
    public void insertar(Linea linea) throws InstanciaExisteException {
        if (existe(linea.getCodigo())) {
            throw new InstanciaExisteException("La línea con código " + linea.getCodigo() + " ya existe en la base de datos.");
        }

        String sql = "INSERT INTO linea (codigo, nombre) VALUES (?, ?)";
        boolean prevuioAutoComit = true;
        try {
            prevuioAutoComit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.setString(2, linea.getNombre());
                ps.executeUpdate();
                LOGGER.info("Línea insertada en PostgreSQL: " + linea.getCodigo());
            }
            insertarLineaParada(conn, linea);

            conn.commit();
            LOGGER.info("Transacción de inserción de línea " + linea.getCodigo() + " completada.");
        } catch (SQLException e) {
            LOGGER.error("Error al insertar la línea " + linea.getCodigo(), e);
            if(conn!=null){
                try {
                    conn.rollback();
                    LOGGER.info("Transacción de inserción de línea " + linea.getCodigo() + " revertida.");
                } catch (SQLException ex) {
                    LOGGER.error("Error al revertir la transacción de inserción de línea " + linea.getCodigo(), ex);
                }
            }
        }
    }

    @Override
    public void actualizar(Linea linea) {
        String sql = "UPDATE linea SET nombre = ? WHERE codigo = ?";
        boolean previoAutoComit = true;
        try {
            previoAutoComit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getNombre());
                ps.setString(2, linea.getCodigo());
                ps.executeUpdate();
                LOGGER.info("Línea actualizada en PostgreSQL: " + linea.getCodigo());
            }
            actualizarLineaParada(conn, linea);
            conn.commit();
            LOGGER.info("Transacción de actualización de línea " + linea.getCodigo() + " completada.");
        } catch (SQLException e) {
            LOGGER.error("Error al actualizar la línea " + linea.getCodigo(), e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.error("Rollback falló", ex); }
            }
        }
    }

    @Override
    public void borrar(Linea linea) throws InstanciaNoExisteException {
        if (!existe(linea.getCodigo())) {
            throw new InstanciaNoExisteException("La línea con código " + linea.getCodigo() + " no existe en la base de datos.");
        }
        String sql = "DELETE FROM linea WHERE codigo = ?";
        boolean previoAutoComit = true;
        try {
            previoAutoComit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            borrarLineaParada(conn, linea);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.executeUpdate();
                LOGGER.info("Línea borrada en PostgreSQL: " + linea.getCodigo());
            }
            conn.commit();
            LOGGER.info("Transacción borrar línea + relacion completada: " + linea.getCodigo());
        } catch (SQLException e) {
            LOGGER.info("Línea borrada en PostgreSQL: " + linea.getCodigo());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.error("Rollback falló", ex); }
            }
        }
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        Map<String, Linea> resultado = new HashMap<>();
        ParadaDAO paradaDAO = new ParadaPostgresqlDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        String sqlLineas = "SELECT codigo, nombre FROM linea";
        String sqlParadasLinea = "SELECT linea AS cod_linea, parada AS cod_parada " +
                "FROM linea_parada ORDER BY linea, secuencia";
        String sqlFrecuencias = "SELECT linea AS cod_linea, diasemana, hora FROM linea_frecuencia";

        try {
            try (PreparedStatement psLineas = conn.prepareStatement(sqlLineas);
                 ResultSet rsLineas = psLineas.executeQuery()) {

                Map<String, List<Parada>> lineasParadas = new HashMap<>();
                Map<String, List<FrecuenciaData>> frecuenciasPorLinea = new HashMap<>();
                
                try (PreparedStatement psPL = conn.prepareStatement(sqlParadasLinea);
                     ResultSet rsPL = psPL.executeQuery()) {
                    while (rsPL.next()) {
                        String codLinea = rsPL.getString("cod_linea");
                        int codParada = rsPL.getInt("cod_parada");
                        Parada parada = paradas.get(codParada);
                        if (parada != null) {
                            lineasParadas.computeIfAbsent(codLinea, k -> new ArrayList<>()).add(parada);
                        }
                    }
                }

                try (PreparedStatement psFreq = conn.prepareStatement(sqlFrecuencias);
                     ResultSet rsFreq = psFreq.executeQuery()) {
                    while (rsFreq.next()) {
                        String codLinea = rsFreq.getString("cod_linea");
                        int diaSemana = rsFreq.getInt("diasemana");
                        Time hora = rsFreq.getTime("hora");
                        if (hora != null) {
                            frecuenciasPorLinea
                                    .computeIfAbsent(codLinea, k -> new ArrayList<>())
                                    .add(new FrecuenciaData(diaSemana, hora.toLocalTime()));
                        }
                    }
                }

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
            LOGGER.error("Error al buscar todas las líneas", e);
        }
        return resultado;
    }

    private boolean existe(String codigo) {
        String sql = "SELECT 1 FROM linea WHERE codigo = ?";
        try (Connection conn = BDConexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Si hay algún resultado, existe
            }
        } catch (SQLException e) {
            LOGGER.error("Error al verificar existencia de la línea " + codigo, e);
        }
        return false;
    }

    private void insertarLineaParada(Connection conn, Linea linea) {
        String sql = "INSERT INTO linea_parada (linea, parada, secuencia) VALUES (?, ?, ?)";
        List<Parada> paradas = linea.getParadas();
        try {
            for (int i = 0; i < paradas.size(); i++) {
                Parada parada = paradas.get(i);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, linea.getCodigo());
                    ps.setInt(2, Integer.parseInt(parada.getCodigo()));
                    ps.setInt(3, i + 1); // orden, comienza en 1
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error al insertar paradas para la línea " + linea.getCodigo(), e);
        }
    }

    private void actualizarLineaParada(Connection conn, Linea linea){
        borrarLineaParada(conn, linea);
        insertarLineaParada(conn, linea);

    }
    private void borrarLineaParada(Connection conn, Linea linea){
        String sql = "DELETE FROM linea_parada WHERE linea = ?";
        try {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, linea.getCodigo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Error al borrar paradas para la línea " + linea.getCodigo(), e);
        }
    }

    private record FrecuenciaData(int diaSemana, LocalTime hora) {
    }

}
