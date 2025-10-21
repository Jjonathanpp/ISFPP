package colectivo.TestSecuencial;

import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

class ParadaSecuencialDAOTest {

    private ParadaSecuencialDAO paradaDAO;
    private Path filePath;
    private String backup;

    private static final String PID = "9001";

    @BeforeEach
    void setUp() throws IOException {
        paradaDAO = new ParadaSecuencialDAO();
        ResourceBundle rb = ResourceBundle.getBundle("config");
        filePath = Path.of("src/resources", rb.getString("parada"));
        backup = Files.exists(filePath) ? Files.readString(filePath) : "";
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.writeString(filePath, backup == null ? "" : backup);
    }

    @Test
    void testInsertarActualizarBorrar() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        // Limpieza previa si existía
        try { paradaDAO.borrar(new Parada(PID, "tmp", 0, 0)); } catch (Exception ignored) {}

        // Insertar
        Parada p = new Parada(PID, "Parada Test 9001", -42.30, -65.30);
        paradaDAO.insertar(p);
        Map<Integer, Parada> todas = paradaDAO.buscarTodos();
        assertTrue(todas.containsKey(Integer.parseInt(PID)));

        // Actualizar
        Parada pUpd = new Parada(PID, "Parada Test 9001 (upd)", -42.31, -65.31);
        paradaDAO.actualizar(pUpd);
        Parada leida = paradaDAO.buscarTodos().get(Integer.parseInt(PID));
        assertNotNull(leida);
        assertEquals("Parada Test 9001 (upd)", leida.getDireccion(), "Debe reflejar el nombre actualizado");

        // Borrar
        paradaDAO.borrar(pUpd);
        assertFalse(paradaDAO.buscarTodos().containsKey(Integer.parseInt(PID)));
    }
}
