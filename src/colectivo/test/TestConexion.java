package colectivo.test;

import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import colectivo.conexion.BDConexion;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConexion {
    @Test
    public void testGetConnectionNotNull() throws SQLException {
        Connection conn = BDConexion.getConnection();
        Assertions.assertNotNull(conn, "La conexión no debe ser null");
        Assertions.assertFalse(conn.isClosed(), "La conexión debe estar abierta");
    }

    @AfterAll
    public static void cerrarConexion() {

        try {
//            BDConexion.MiShDwnHook;
        } catch (Exception e) {
            Assertions.fail("No se pudo cerrar la conexión: " + e.getMessage());
        }
    }
}
