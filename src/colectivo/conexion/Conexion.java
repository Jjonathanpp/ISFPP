package colectivo.conexion;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {
    private static final Logger LOGGER = Logger.getLogger(Conexion.class);

    private static Conexion instancia;
    private Connection conn;

    // Parámetros de conexión proporcionados:
    private static final String HOST = "pgs.fi.mdn.unp.edu.ar";
    private static final String PORT = "30000";
    private static final String DB   = "bd1";
    private static final String USER = "estudiante";
    private static final String PASS = "estudiante";

    private Conexion() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DB);
        this.conn = DriverManager.getConnection(url, USER, PASS);

        // Establecer el esquema por defecto
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path TO isfpp_poo_2025");
        }
    }

    public static synchronized Conexion getInstancia() throws SQLException {
        if (instancia == null || instancia.conn.isClosed()) {
            instancia = new Conexion();
        }
        return instancia;
    }

    public Connection getConnection() {
        return conn;
    }

    //Cerrar la conexión si esta abierta, es seguro llamarlo varias veces
    public static void cerrar() {
        if(instancia == null) return;
        if(instancia.conn != null){
            try {
                if(!instancia.conn.isClosed()){
                    instancia.conn.close();
                    LOGGER.info("Cerrando la conexión a la base de datos.");
                }
            } catch (SQLException e) {
                LOGGER.error("Error al verificar el estado de la conexión.", e);
            }
        }
        instancia = null;
    }
}

