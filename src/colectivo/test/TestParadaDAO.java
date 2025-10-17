package colectivo.test;

import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.modelo.Parada;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

import static java.lang.Integer.parseInt;

public class TestParadaDAO {
    ParadaSecuencialDAO paradaDAO = new ParadaSecuencialDAO();
    private static final String TEST_PARADA_ID = String.valueOf(200);
    private static final String TEST_PARADA_NOMBRE = "Test Parada";
    private static final double TEST_LAT = -42.0, TEST_LON = -65.0;

    @AfterEach
    void cleanup() {
        // Borra la parada de test si existe
        Parada p = new Parada(TEST_PARADA_ID, TEST_PARADA_NOMBRE, TEST_LAT, TEST_LON);
        paradaDAO.borrar(p);
    }


    @Test
    public void testInsertar() {
        Parada p = new Parada(TEST_PARADA_ID, TEST_PARADA_NOMBRE, TEST_LAT, TEST_LON);
        paradaDAO.insertar(p);
        Assertions.assertTrue(paradaDAO.buscarTodos().containsKey(parseInt(TEST_PARADA_ID)));
        paradaDAO.borrar(p);
    }

    @Test
    public void testActualizar() {
        Parada p = new Parada(TEST_PARADA_ID, TEST_PARADA_NOMBRE, TEST_LAT, TEST_LON);
        paradaDAO.insertar(p);
        p.setDireccion(TEST_PARADA_NOMBRE + " UPDATED");
        paradaDAO.actualizar(p);
        Assertions.assertEquals(TEST_PARADA_NOMBRE + " UPDATED", paradaDAO.buscarTodos().get(parseInt(TEST_PARADA_ID)).getDireccion());
        paradaDAO.borrar(p);
    }

    @Test
    public void testBuscarTodos() {
        Parada p = new Parada(TEST_PARADA_ID, TEST_PARADA_NOMBRE, TEST_LAT, TEST_LON);
        paradaDAO.insertar(p);
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();
        Assertions.assertNotNull(paradas);
        Assertions.assertTrue(paradas.containsKey(parseInt(TEST_PARADA_ID)));
        paradaDAO.borrar(p);
    }

    @Test
    public void testBorrar() {
        Parada p = new Parada(TEST_PARADA_ID, TEST_PARADA_NOMBRE, TEST_LAT, TEST_LON);
        paradaDAO.borrar(p);
        Assertions.assertFalse(paradaDAO.buscarTodos().containsKey(TEST_PARADA_ID));
    }
}
