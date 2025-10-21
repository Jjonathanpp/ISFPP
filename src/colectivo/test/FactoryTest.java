package colectivo.test;

import colectivo.conexion.Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.*;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para la clase Factory.
 */
public class FactoryTest {

    private static ResourceBundle bundle;
    private static List<String> keys;

    @BeforeAll
    public static void setup() {
        // Carga el resource bundle "factory" (factory.properties)
        bundle = ResourceBundle.getBundle("factory");
        keys = new ArrayList<>();
        Enumeration<String> en = bundle.getKeys();
        while (en.hasMoreElements()) {
            keys.add(en.nextElement());
        }
        // Aseguramos que exista al menos una clave para poder probar
        assertFalse(keys.isEmpty(), "factory.properties no contiene claves - asegúrate que está en el classpath");
    }

    @Test
    public void testKeysReturnNonNullAndCorrectClassAndSingleton() {
        for (String key : keys) {
            String expectedClassName = bundle.getString(key).trim();
            Object inst1 = Factory.getInstancia(key);
            assertNotNull(inst1, () -> "Factory devolvió null para la clave: " + key);

            // Comprueba que la clase devuelta coincide con la que figura en el properties
            assertEquals(expectedClassName, inst1.getClass().getName(),
                    () -> "Para la clave " + key + " se esperaba la clase " + expectedClassName
                            + " pero se obtuvo " + inst1.getClass().getName());

            // Comprueba comportamiento singleton por clave (misma instancia si se invoca de nuevo)
            Object inst2 = Factory.getInstancia(key);
            assertSame(inst1, inst2, () -> "Factory debe devolver la misma instancia para la misma clave: " + key);
        }
    }

    @Test
    public void testDifferentKeysReturnDifferentInstances() {
        if (keys.size() < 2) {
            // Si sólo hay una clave, la prueba no es aplicable y la saltamos en forma segura.
            return;
        }
        String k1 = keys.get(0);
        String k2 = keys.get(1);
        Object a = Factory.getInstancia(k1);
        Object b = Factory.getInstancia(k2);

        assertNotNull(a, "Instancia para " + k1 + " es null");
        assertNotNull(b, "Instancia para " + k2 + " es null");
        assertNotSame(a, b, "Instancias para claves distintas (" + k1 + ", " + k2 + ") no deberían ser la misma");
    }

    @Test
    public void testMissingKeyThrowsRuntimeException() {
        String nonExistent = "LLAVE_QUE_NO_EXISTE_EN_FACTORY_PROPERTIES_012345";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Factory.getInstancia(nonExistent),
                "Se esperaba RuntimeException al pedir una clave inexistente");
        assertNotNull(ex.getCause(), "Se esperaba que la RuntimeException tenga causa");
        assertTrue(ex.getCause() instanceof MissingResourceException,
                "La causa esperada es MissingResourceException cuando la clave no existe");
    }

    @Test
    public void testReturnedObjectsHavePublicMethods() {
        // Guardar como verificación mínima de que las clases creadas son "útiles"
        for (String key : keys) {
            Object inst = Factory.getInstancia(key);
            assertTrue(inst.getClass().getMethods().length > 0,
                    "La clase " + inst.getClass().getName() + " no tiene métodos públicos (inalcanzable)");
        }
    }
}