package colectivo.test;

import colectivo.conexion.Conexion;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConexion {
    @Test
    public void testGetConnectionNotNull() throws SQLException {
        Connection conn = Conexion.getInstancia().getConnection();
        Assertions.assertNotNull(conn, "La conexión no debe ser null");
        Assertions.assertFalse(conn.isClosed(), "La conexión debe estar abierta");
    }

    @AfterAll
    public static void cerrarConexion() {
        // Intentamos cerrar la conexión después de todos los tests
        try {
            Conexion.cerrar();
        } catch (Exception e) {
            Assertions.fail("No se pudo cerrar la conexión: " + e.getMessage());
        }
    }
}
