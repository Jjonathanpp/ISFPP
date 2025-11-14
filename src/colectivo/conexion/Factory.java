package colectivo.conexion;


import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;


import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

public class Factory {

    private static final String FACTORY_BUNDLE_NAME = "factory";
    private static final ConcurrentHashMap<String, Object> INSTANCIAS = new ConcurrentHashMap<>();
    private static final Logger LOG = LogManager.getLogger(Factory.class);

    private Factory() {
        throw new AssertionError("No instanciable");
    }

    /**
     * Obtiene una instancia type-safe del componente configurado
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstancia(String objName, Class<T> expectedType) {
        String key = normalizeKey(objName);
        if (expectedType == null) {
            throw new IllegalArgumentException("El tipo esperado no puede ser nulo.");
        }

        Object instance = INSTANCIAS.computeIfAbsent(key, Factory::crearInstancia);

        if (!expectedType.isInstance(instance)) {
            String error = String.format("ERROR CONFIGURACIÓN: %s → Esperado: %s, Obtenido: %s", key,
                    expectedType.getName(), instance.getClass().getName());
            LOG.error(error);
            throw new ClassCastException(error);
        }

        return (T) instance;
    }

    public static Object getInstancia(String objName) {
        return getInstancia(objName, Object.class);
    }

    public static void clearCache() {
        INSTANCIAS.clear();
        LOG.info("Cache de Factory limpiado");
    }

    public static <T> T reloadInstancia(String objName, Class<T> expectedType) {
        String key = normalizeKey(objName);
        INSTANCIAS.remove(key);
        return getInstancia(key, expectedType);
    }

    public static Object reloadInstancia(String objName) {
        return reloadInstancia(objName, Object.class);
    }

    private static Object crearInstancia(String clave) {
        try {
            LOG.info("Creando instancia para: " + clave);
            ResourceBundle resourceBundle = ResourceBundle.getBundle(FACTORY_BUNDLE_NAME);

            if (!resourceBundle.containsKey(clave)) {
                throw new IllegalArgumentException("Clave no encontrada en " + FACTORY_BUNDLE_NAME + ": " + clave);
            }

            String className = resourceBundle.getString(clave);
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            LOG.debug("Instancia creada: " + clave + " → " + className);
            return instance;
        } catch (Exception ex) {
            LOG.error("Error creando instancia para: " + clave, ex);
            throw new RuntimeException("Error Factory al crear: " + clave, ex);
        }
    }


    private static String normalizeKey(String objName) {
        if (objName == null) {
            throw new IllegalArgumentException("El nombre de la instancia no puede ser nulo.");
        }

        String key = objName.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la instancia no puede estar vacío.");
        }
        return key;
    }
}
