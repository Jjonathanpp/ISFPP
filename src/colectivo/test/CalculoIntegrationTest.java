package colectivo.test;

import colectivo.conexion.Factory;
import colectivo.dao.ParadaDAO;
import colectivo.logica.Calculo;
import colectivo.modelo.Parada;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración ligeros que verifican que:
 * - Factory crea las implementaciones configuradas en factory.properties,
 * - las DAOs devuelven datos (paradas) y
 * - Calculo.calcularRecorrido puede ejecutarse usando las DAOs configuradas.
 *
 * NOTAS:
 * - Estos tests NO usan fakes: usan las implementaciones que declares en
 *   src/test/resources/factory.properties o src/main/resources/factory.properties.
 * - Si usas implementaciones PostgreSQL asegúrate de tener la BD y la conexión configurada;
 *   si no, los tests pueden fallar al intentar obtener datos de la BD.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CalculoIntegrationTest {

    @BeforeEach
    public void clearFactoryCache() throws Exception {
        // La Factory usa una Hashtable privada llamada "instancias".
        // Limpiamos la cache entre tests para forzar re-lectura del bundle/instanciación.
        try {
            Field f = Factory.class.getDeclaredField("instancias");
            f.setAccessible(true);
            Object inst = f.get(null); // campo estático
            if (inst instanceof Hashtable) {
                ((Hashtable<?, ?>) inst).clear();
            }
        } catch (NoSuchFieldException nsfe) {
            // Si la implementación cambia, no fallamos; los tests seguirán intentando.
        }
    }

    @Test
    @DisplayName("Factory debe devolver DAOs y ParadaDAO.buscarTodos() no debe estar vacío")
    public void testFactoryProvidesParadaDAOAndParadasExist() {
        ResourceBundle rb = ResourceBundle.getBundle("factory");
        assertTrue(rb.containsKey("PARADA"), "factory.properties debe contener la clave PARADA");

        String paradaClassName = rb.getString("PARADA").trim();
        assertNotNull(paradaClassName, "El valor para PARADA no debe ser nulo");

        // Verificamos que la clase indicada existe en el classpath
        try {
            Class.forName(paradaClassName);
        } catch (ClassNotFoundException e) {
            fail("No se encontró la clase indicada para PARADA en factory.properties: " + paradaClassName);
        }

        // Instanciamos vía Factory y comprobamos que buscarTodos() devuelve datos
        Object obj = Factory.getInstancia("PARADA");
        assertNotNull(obj, "Factory devolvió null para PARADA");
        assertTrue(obj instanceof ParadaDAO, "La instancia devuelta para PARADA debe implementar ParadaDAO");

        ParadaDAO paradaDAO = (ParadaDAO) obj;
        Map<Integer, Parada> mapa = paradaDAO.buscarTodos();
        assertNotNull(mapa, "paradaDAO.buscarTodos() no debe devolver null");
        assertFalse(mapa.isEmpty(), "paradaDAO.buscarTodos() debería devolver al menos una parada para poder probar Calculo");
    }

    @Test
    @DisplayName("Calculo.calcularRecorrido se ejecuta con las DAOs configuradas y devuelve lista (no lanza)")
    public void testCalculoRunsWithConfiguredDAOs() {
        // Obtenemos ParadaDAO desde la Factory
        ParadaDAO paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
        assertNotNull(paradaDAO, "No se pudo obtener ParadaDAO desde Factory");

        Map<Integer, Parada> mapaParadas = paradaDAO.buscarTodos();
        assertNotNull(mapaParadas, "paradaDAO.buscarTodos() devolvió null");
        assertFalse(mapaParadas.isEmpty(), "paradaDAO.buscarTodos() debe contener paradas para ejecutar el test");

        // Elegimos dos paradas distintas existentes (si hay sólo una, no podemos testear rutas entre dos)
        List<Parada> paradas = mapaParadas.values().stream().collect(Collectors.toList());
        if (paradas.size() < 2) {
            // Si solo hay una parada disponible, no podemos calcular recorrido entre dos; marcamos como skipped.
            Assumptions.assumeTrue(false, "No hay al menos dos paradas disponibles para ejecutar el test de recorrido");
        }

        Parada origen = paradas.get(0);
        Parada destino = paradas.get(1);

        // Ejecutar el cálculo; usamos hora temprana (08:00) y diaSemana=1
        List<List<colectivo.modelo.Recorrido>> rutas = null;
        try {
            rutas = Calculo.calcularRecorrido(origen, destino, 1, LocalTime.of(8, 0), null);
        } catch (Exception e) {
            // Si lanza excepción, el test debe fallar — mostramos la causa para debugging
            fail("Calculo.calcularRecorrido lanzó excepción: " + e.getClass().getName() + " - " + e.getMessage());
        }

        // Verificaciones básicas
        assertNotNull(rutas, "calcularRecorrido no debe devolver null (puede devolver lista vacía si no hay rutas)");
        assertTrue(rutas instanceof List, "El resultado debe ser una List de rutas");

        // Si se encontraron rutas, comprobamos la estructura mínima
        if (!rutas.isEmpty()) {
            List<colectivo.modelo.Recorrido> primeraRuta = rutas.get(0);
            assertNotNull(primeraRuta, "La primera ruta no debe ser null");
            assertFalse(primeraRuta.isEmpty(), "La primera ruta no debe estar vacía");
            // la primera etapa puede ser caminata (linea == null) o transporte; no afirmamos más por dependencia de datos
        }
    }

    @Test
    public void testLineasTienenFrecuencias() {
        colectivo.dao.LineaDAO lineaDAO = (colectivo.dao.LineaDAO) Factory.getInstancia("LINEA");
        Map<String, colectivo.modelo.Linea> lineas = lineaDAO.buscarTodos();
        assertNotNull(lineas);
        assertFalse(lineas.isEmpty(), "No hay líneas cargadas");

        for (colectivo.modelo.Linea l : lineas.values()) {
            assertNotNull(l.getFrecuencias(), "Linea " + l.getCodigo() + " tiene getFrecuencias() == null");
            assertFalse(l.getFrecuencias().isEmpty(), "Linea " + l.getCodigo() + " NO tiene frecuencias cargadas");
        }
    }
}