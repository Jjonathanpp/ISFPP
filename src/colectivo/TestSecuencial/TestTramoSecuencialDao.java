package colectivo.TestSecuencial;

import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.dao.secuencial.TramoSecuencialDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

class TramoSecuencialDAOTest {

    private ParadaSecuencialDAO paradaDAO;
    private TramoSecuencialDAO tramoDAO;

    private Path tramoPath;
    private Path paradaPath;
    private String tramoBackup;
    private String paradaBackup;

    private static final String PID1 = "9001";
    private static final String PID2 = "9002";
    private static final int TIPO = 1; // Colectivo
    private static final String TRAMO_KEY = PID1 + ";" + PID2 + ";" + TIPO;

    @BeforeEach
    void setUp() throws IOException, InstanciaExisteEnBDException {
        paradaDAO = new ParadaSecuencialDAO();
        tramoDAO  = new TramoSecuencialDAO();

        ResourceBundle rb = ResourceBundle.getBundle("config");
        paradaPath = Path.of("src/resources", rb.getString("parada"));
        tramoPath  = Path.of("src/resources", rb.getString("tramo"));

        paradaBackup = Files.exists(paradaPath) ? Files.readString(paradaPath) : "";
        tramoBackup  = Files.exists(tramoPath) ? Files.readString(tramoPath) : "";

        // Garantizar que existan las paradas usadas por los tramos
        try { paradaDAO.borrar(new Parada(PID1, "", 0, 0)); } catch (Exception ignored) {}
        try { paradaDAO.borrar(new Parada(PID2, "", 0, 0)); } catch (Exception ignored) {}
        paradaDAO.insertar(new Parada(PID1, "P1 test", -42.30, -65.30));
        paradaDAO.insertar(new Parada(PID2, "P2 test", -42.40, -65.40));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.writeString(tramoPath,  tramoBackup == null ? "" : tramoBackup);
        Files.writeString(paradaPath, paradaBackup == null ? "" : paradaBackup);
    }

    @Test
    void testInsertarActualizarBorrar() throws InstanciaNoExisteEnBDException {
        Parada p1 = new Parada(PID1, "P1 test", -42.30, -65.30);
        Parada p2 = new Parada(PID2, "P2 test", -42.40, -65.40);

        // Limpieza previa si ya existía
        try { tramoDAO.borrar(new Tramo(100, TIPO, p1, p2)); } catch (Exception ignored) {}

        // Insertar
        Tramo t = new Tramo(100, TIPO, p1, p2);
        tramoDAO.insertar(t);
        assertTrue(tramoDAO.buscarTodos().containsKey(TRAMO_KEY), "Debe existir el tramo insertado");

        // Actualizar (cambiar el tiempo)
        Tramo tUpd = new Tramo(180, TIPO, p1, p2);
        tramoDAO.actualizar(tUpd);
        Tramo leido = tramoDAO.buscarTodos().get(TRAMO_KEY);
        assertNotNull(leido);
        assertEquals(180, leido.getTiempo(), "El tiempo debe reflejar la actualización");

        // Borrar
        tramoDAO.borrar(tUpd);
        assertFalse(tramoDAO.buscarTodos().containsKey(TRAMO_KEY), "Debe haberse eliminado el tramo");
    }
}
