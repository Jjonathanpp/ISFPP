package colectivo.test;

import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Map;

public class TestLineaDAO {
    LineaSecuencialDAO lineaDAO = new LineaSecuencialDAO();
    ParadaSecuencialDAO paradaDAO = new ParadaSecuencialDAO();
    private static final String TEST_LINEA_ID = "TLINEA";
    private static final String TEST_LINEA_NOMBRE = "Linea Test";
    private static final String TEST_PARADA_ID1 = "201";
    private static final String TEST_PARADA_ID2 = "202";


    @Test
    public void testInsertar() {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        paradaDAO.insertar(new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1));
        paradaDAO.insertar(new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2));
        lineaDAO.insertar(l);
        System.out.println(lineaDAO.buscarTodos().keySet());
        Assertions.assertTrue(lineaDAO.buscarTodos().containsKey("TLINEA"));
        lineaDAO.borrar(l);
    }

    @Test
    public void testActualizar() {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        lineaDAO.insertar(l);
        l.setNombre(TEST_LINEA_NOMBRE + " UPDATED");
        lineaDAO.actualizar(l);
        Assertions.assertEquals(TEST_LINEA_NOMBRE + " UPDATED", lineaDAO.buscarTodos().get(TEST_LINEA_ID).getNombre());
    }

    @Test
    public void testBuscarTodos() {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        lineaDAO.insertar(l);
        Map<String, Linea> lineas = lineaDAO.buscarTodos();
        Assertions.assertNotNull(lineas);
        Assertions.assertTrue(lineas.containsKey(TEST_LINEA_ID));
    }

    @Test
    public void testBorrar() {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        lineaDAO.insertar(l);
        lineaDAO.borrar(l);
        Assertions.assertFalse(lineaDAO.buscarTodos().containsKey(TEST_LINEA_ID));
    }
}
