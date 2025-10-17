package colectivo.conexion;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {
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

    public static void cerrar() {
        if (instancia != null && instancia.conn != null) {
            try {
                instancia.conn.close();
            } catch (SQLException ignored) {}
        }
    }
}

