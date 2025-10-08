package colectivo.conexion;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static Connection conn;

    private static final String HOST =  System.getenv().getOrDefault("PGHOST", "ep-hidden-sun-ac0ofa40-pooler.sa-east-1.aws.neon.tech");
    private static final String DB   = System.getenv().getOrDefault("PGDATABASE", "neondb");
    private static final String USER = System.getenv().getOrDefault("PGUSER", "app_reader");
    private static final String PASS = System.getenv().getOrDefault("PGPASSWORD", "camisa_pelota_lapiz");
    private static final String SSLMODE = System.getenv().getOrDefault("PGSSLMODE", "require"); // require/verify-full

    //para conectar a la base de datos
    public static Connection getConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:5432/%s?sslmode=%s", HOST, DB, SSLMODE);
        return DriverManager.getConnection(url, USER, PASS);
    }

    //para cerrar conexión
    public static void cerrar(Connection c) {
        if (c != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
    }
}

