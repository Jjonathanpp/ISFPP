package colectivo.dao.postgresql;


import colectivo.conexion.Conexion;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
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

/**
 * Explicación de porque se repite en cada metodo la creación de un nuevo objeto Connection:
 * Es claro y seguro para la concurrencia y evita fugas, si se tiene a Connection como atributo de clase hay que gestionar
 * su ciclo de vida, sincronización y transacciones de forma explicita (y cerrar la conexión al final). Si no se hace bien
 * puede llevar a inconsistencias, bloqueos, fugas y problemas de transacción.
 * (PUEDE QUE CON ESTO SE REFIERE AL HILO DESCONECTOR DE LA BD)
 */

public class LineaPostgresqlDAO implements LineaDAO {

    private static final Logger LOGGER = Logger.getLogger(LineaPostgresqlDAO.class);

    @Override
    public void insertar(Linea linea) throws InstanciaExisteEnBDException {
        if (existe(linea.getCodigo())) {
            throw new InstanciaExisteEnBDException("La línea con código " + linea.getCodigo() + " ya existe en la base de datos.");
        }

        String sql = "INSERT INTO linea (codigo, nombre) VALUES (?, ?)";
        Connection conn = null;
        boolean prevuioAutoComit = true;
        try {
            conn = Conexion.getInstancia().getConnection();
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
        Connection conn = null;
        boolean previoAutoComit = true;
        try {
            conn = Conexion.getInstancia().getConnection();
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
    public void borrar(Linea linea) throws InstanciaNoExisteEnBDException {
        if (!existe(linea.getCodigo())) {
            throw new InstanciaNoExisteEnBDException("La línea con código " + linea.getCodigo() + " no existe en la base de datos.");
        }
        String sql = "DELETE FROM linea WHERE codigo = ?";
        Connection conn = null;
        boolean previoAutoComit = true;
        try {
            conn = Conexion.getInstancia().getConnection();
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
            LOGGER.error("Error al buscar todas las líneas", e);
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
            LOGGER.error("Error al verificar existencia de la línea " + codigo, e);
        }
        return false;
    }
    //================================= DAO DE LineaParada =================================
    private void insertarLineaParada(Connection conn, Linea linea) {
        String sql = "INSERT INTO linea_parada (codigo_linea, codigo_parada, orden) VALUES (?, ?, ?)";
        List<Parada> paradas = linea.getParadas();
        try {
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
            LOGGER.error("Error al insertar paradas para la línea " + linea.getCodigo(), e);
        }
    }

    private void actualizarLineaParada(Connection conn, Linea linea){
        borrarLineaParada(conn, linea);
        insertarLineaParada(conn, linea);

    }
    private void borrarLineaParada(Connection conn, Linea linea){
        String sql = "DELETE FROM linea_parada WHERE codigo_linea = ?";
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
