package colectivo.conexion;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {
    private static final Logger LOGGER = Logger.getLogger(Conexion.class);

    private static Conexion instancia;
    //private Connection conn;

    // URL JDBC
    private final String url;

    // Parámetros de conexión proporcionados:
    private static final String HOST = "pgs.fi.mdn.unp.edu.ar";
    private static final String PORT = "30000";
    private static final String DB   = "bd1";
    private static final String USER = "estudiante";
    private static final String PASS = "estudiante";

    private Conexion() throws SQLException {
        this.url = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DB);
        // Opcional: cargar el driver explícitamente (no suele ser necesario con JDBC 4+)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Driver PostgreSQL no encontrado explícitamente (probablemente no hace falta).", e);
        }
    }

    public static synchronized Conexion getInstancia() throws SQLException {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, USER, PASS);
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path TO isfpp_poo_2025");
        } catch (SQLException e) {
            // Si falló el set del search_path, cerramos la conexión y relanzamos
            try { conn.close(); } catch (Exception ex) { LOGGER.warn("Error cerrando conexión tras fallo en search_path", ex); }
            throw e;
        }
        return conn;
    }

    //Cerrar la conexión si esta abierta, es seguro llamarlo varias veces
    public static void cerrar() {
        // No hay conexión singleton abierta en esta implementación (cada getConnection crea una nueva).
        // Si en el futuro usas un DataSource/pool, ciérralo aquí.
        LOGGER.info("Conexion.cerrar() llamado. No hay conexión singleton que cerrar en la implementación actual.");
        instancia = null;
    }

    /**
     * Explicación clase Conexion:
     * Es una fabrica singleton que encapsula los parametros de conexión y crea nuevas conexiones JDBC
     * bajo demanda.
     * Cada llamada a getConnection() devuelve una Connection nueva (no hay una Connection compartida).
     * NO es un singleton de Connection porque devolver una conexión compartida trae problemas de transacciones.
     * Y se hace singleton para evitar repetir la construcción de URL y el intento de carga del driver.
     * ¿Por qué no se comparten conexiones?
     * Porque las conexiones JDBC no son thread-safe y compartirlas puede causar problemas de concurrencia.
     */
}

