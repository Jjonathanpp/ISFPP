package colectivo.test;

import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.dao.secuencial.TramoSecuencialDAO;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class TestTramoDAO {
    TramoSecuencialDAO tramoDAO = new TramoSecuencialDAO();
    ParadaSecuencialDAO paradaDAO = new ParadaSecuencialDAO();
    private static final String TEST_PARADA_ID1 = "203";
    private static final String TEST_PARADA_ID2 = "204";
    private static final String TEST_TRAMO_KEY = TEST_PARADA_ID1 + "-" + TEST_PARADA_ID2;

    @AfterEach
    public void cleanup() {
        tramoDAO.borrar(new Tramo(100, 1,
                new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3),
                new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4)
        ));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3));
        paradaDAO.borrar(new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4));
    }

    @Test
    public void testInsertar() {
        paradaDAO.insertar(new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3));
        paradaDAO.insertar(new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4));
        Tramo t = new Tramo(100, 1,
                new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3),
                new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4)
        );
        tramoDAO.insertar(t);
        Assertions.assertTrue(tramoDAO.buscarTodos().containsKey(TEST_TRAMO_KEY));
    }

    @Test
    public void testActualizar() {
        paradaDAO.insertar(new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3));
        paradaDAO.insertar(new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4));
        Tramo t = new Tramo(100, 1,
                new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3),
                new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4)
        );
        tramoDAO.insertar(t);
        t.setTiempo(200);
        tramoDAO.actualizar(t);
        Assertions.assertEquals(200, tramoDAO.buscarTodos().get(TEST_TRAMO_KEY).getTiempo());
    }

    @Test
    public void testBuscarTodos() {
        Tramo t = new Tramo(100, 1,
                new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3),
                new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4)
        );
        tramoDAO.insertar(t);
        Map<String, Tramo> tramos = tramoDAO.buscarTodos();
        Assertions.assertNotNull(tramos);
        Assertions.assertTrue(tramos.containsKey(TEST_TRAMO_KEY));
    }

    @Test
    public void testBorrar() {
        Tramo t = new Tramo(100, 1,
                new Parada(TEST_PARADA_ID1, "P1", -42.3, -65.3),
                new Parada(TEST_PARADA_ID2, "P2", -42.4, -65.4)
        );
        tramoDAO.insertar(t);
        tramoDAO.borrar(t);
        Assertions.assertFalse(tramoDAO.buscarTodos().containsKey(TEST_TRAMO_KEY));
    }
}
