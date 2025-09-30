package colectivo.conexion;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static Connection conn;

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tu_base", "usuario", "contraseña");
        }
        return conn;
    }
}

