package colectivo.TestSecuencial;

import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LineaSecuencialDAOTest {

    private ParadaSecuencialDAO paradaDAO;
    private LineaSecuencialDAO lineaDAO;

    private Path lineaPath;
    private Path paradaPath;
    private String lineaBackup;
    private String paradaBackup;

    private static final String PID1 = "9001";
    private static final String PID2 = "9002";
    private static final String LCOD = "LTEST";
    private static final String LNAME = "Línea Test";

    @BeforeEach
    void setUp() throws IOException, InstanciaExisteEnBDException {
        paradaDAO = new ParadaSecuencialDAO();
        lineaDAO  = new LineaSecuencialDAO();

        ResourceBundle rb = ResourceBundle.getBundle("config");
        paradaPath = Path.of("src/resources", rb.getString("parada"));
        lineaPath  = Path.of("src/resources", rb.getString("linea"));

        paradaBackup = Files.exists(paradaPath) ? Files.readString(paradaPath) : "";
        lineaBackup  = Files.exists(lineaPath) ? Files.readString(lineaPath) : "";

        // Asegurar paradas base
        try { paradaDAO.borrar(new Parada(PID1, "", 0, 0)); } catch (Exception ignored) {}
        try { paradaDAO.borrar(new Parada(PID2, "", 0, 0)); } catch (Exception ignored) {}
        paradaDAO.insertar(new Parada(PID1, "P1 test", -42.30, -65.30));
        paradaDAO.insertar(new Parada(PID2, "P2 test", -42.40, -65.40));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.writeString(lineaPath,  lineaBackup == null ? "" : lineaBackup);
        Files.writeString(paradaPath, paradaBackup == null ? "" : paradaBackup);
    }

    @Test
    void testInsertarActualizarBorrar() throws InstanciaExisteEnBDException, InstanciaNoExisteEnBDException {
        // Limpieza previa
        try { lineaDAO.borrar(new Linea(LCOD, LNAME, List.of(new Parada(PID1, "", 0, 0), new Parada(PID2, "", 0, 0)))); } catch (Exception ignored) {}

        // Construir la línea con al menos 2 paradas
        Parada p1 = paradaDAO.buscarTodos().get(Integer.parseInt(PID1));
        Parada p2 = paradaDAO.buscarTodos().get(Integer.parseInt(PID2));
        assertNotNull(p1);
        assertNotNull(p2);

        Linea l = new Linea(LCOD, LNAME, List.of(p1, p2));

        // Insertar
        lineaDAO.insertar(l);
        Map<String, Linea> todas = lineaDAO.buscarTodos();
        assertTrue(todas.containsKey(LCOD), "Debe existir la línea insertada");

        // Actualizar (cambiar nombre)
        Linea lUpd = new Linea(LCOD, LNAME + " (upd)", List.of(p1, p2));
        lineaDAO.actualizar(lUpd);
        Linea leida = lineaDAO.buscarTodos().get(LCOD);
        assertNotNull(leida);
        assertEquals(LNAME + " (upd)", leida.getNombre(), "Debe reflejar el nuevo nombre");

        // Borrar
        lineaDAO.borrar(lUpd);
        assertFalse(lineaDAO.buscarTodos().containsKey(LCOD), "Debe eliminarse la línea");
    }
}
