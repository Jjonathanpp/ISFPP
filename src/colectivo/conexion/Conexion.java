package colectivo.conexion;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {
    private static final Logger LOGGER = Logger.getLogger(Conexion.class);

    private static Conexion instancia;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("ShutdownHook: cerrando subsistema de conexión...");
                Conexion.cerrar();
            } catch (Exception e) {
                LOGGER.error("Error durante el cierre de la conexión", e);
            }
        }));
    }

    private final String url;


    private static final String HOST = "pgs.fi.mdn.unp.edu.ar";
    private static final String PORT = "30000";
    private static final String DB   = "bd1";
    private static final String USER = "estudiante";
    private static final String PASS = "estudiante";

    private Conexion() throws SQLException {
        this.url = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DB);
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
            try { conn.close(); } catch (Exception ex) { LOGGER.warn("Error cerrando conexión tras fallo en search_path", ex); }
            throw e;
        }
        return conn;
    }


    public static void cerrar() {
        LOGGER.info("Conexion.cerrar() llamado. No hay conexión singleton que cerrar en la implementación actual.");
        instancia = null;
    }
}

