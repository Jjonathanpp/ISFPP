package colectivo.test;

import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
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
    Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
            new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
            new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
    ));

    @Test
    public void testInsertar() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        paradaDAO.insertar(new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1));
        paradaDAO.insertar(new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2));
        if (!lineaDAO.buscarTodos().containsKey(TEST_LINEA_ID)) {
            lineaDAO.insertar(l);
        }
        System.out.println(lineaDAO.buscarTodos().keySet());
        Assertions.assertTrue(lineaDAO.buscarTodos().containsKey("TLINEA"));
        lineaDAO.borrar(l);
        paradaDAO.borrar(new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2));
    }

    @Test
    public void testActualizar() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        if (!lineaDAO.buscarTodos().containsKey(TEST_LINEA_ID)) {
            lineaDAO.insertar(l);
        }
        l.setNombre(TEST_LINEA_NOMBRE + " UPDATED");
        lineaDAO.actualizar(l);
        Assertions.assertEquals(TEST_LINEA_NOMBRE + " UPDATED", lineaDAO.buscarTodos().get(TEST_LINEA_ID).getNombre());
        paradaDAO.borrar(new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2));
    }

    @Test
    public void testBuscarTodos() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        Linea l = new Linea(TEST_LINEA_ID, TEST_LINEA_NOMBRE, Arrays.asList(
                new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1),
                new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2)
        ));
        if (!lineaDAO.buscarTodos().containsKey(TEST_LINEA_ID)) {
            lineaDAO.insertar(l);
        }
        Map<String, Linea> lineas = lineaDAO.buscarTodos();
        Assertions.assertNotNull(lineas);
        Assertions.assertTrue(lineas.containsKey(TEST_LINEA_ID));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID1, "P1", -42.1, -65.1));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID2, "P2", -42.2, -65.2));
    }

    @Test
    public void testBorrar() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        lineaDAO.borrar(l);
        Assertions.assertFalse(lineaDAO.buscarTodos().containsKey(TEST_LINEA_ID));
    }
}
