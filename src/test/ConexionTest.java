package test;

import colectivo.conexion.Conexion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

class ConexionTest {

    @Test
    void testGetConnection() {
        try (Connection conn = Conexion.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        } catch (Exception e) {
            fail("La conexión a la base de datos falló: " + e.getMessage());
        }
    }

    @Test
    void cerrarCierraConexionActiva() throws Exception {
        Connection c = Conexion.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed(), "La conexión debería estar abierta al crearla");

        Conexion.cerrar(c);

        assertTrue(c.isClosed(), "La conexión debería quedar cerrada después de cerrar()");
    }

    @Test
    void cerrarNoLanzaSiEsNull() {
        assertDoesNotThrow(() -> Conexion.cerrar(null), "cerrar(null) no debe lanzar excepción");
    }

    @Test
    void cerrarEsIdempotente() throws Exception {
        Connection c = Conexion.getConnection();
        Conexion.cerrar(c);                 // primera vez
        assertTrue(c.isClosed());

        // segunda vez no debe lanzar
        assertDoesNotThrow(() -> Conexion.cerrar(c));
        assertTrue(c.isClosed(), "Sigue cerrada tras llamar dos veces");
    }


}