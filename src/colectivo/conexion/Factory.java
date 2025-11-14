package colectivo.conexion;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Factory {

    private static final String FACTORY_BUNDLE_NAME = "factory";
    private static final ConcurrentMap<String, Object> INSTANCIAS = new ConcurrentHashMap<>();

    private Factory() {
        throw new AssertionError("La clase Factory no debe instanciarse.");
    }

    public static <T> T getInstancia(String objName, Class<T> expectedType) {
        String key = normalizeKey(objName);
        if (expectedType == null) {
            throw new IllegalArgumentException("El tipo esperado no puede ser nulo.");
        }

        Object instance = INSTANCIAS.get(key);
        if (instance == null) {
            instance = createInstance(key);
            Object previous = INSTANCIAS.putIfAbsent(key, instance);
            if (previous != null) {
                instance = previous;
            }
        }

        if (!expectedType.isInstance(instance)) {
            throw new IllegalStateException("La instancia asociada a '" + key + "' es de tipo "
                    + instance.getClass().getName() + " y no del tipo esperado " + expectedType.getName() + ".");
        }

        return expectedType.cast(instance);
    }

    public static Object getInstancia(String objName) {
        return getInstancia(objName, Object .class);
    }

    public static void clearCache() {
        INSTANCIAS.clear();
    }

    public static <T> T reloadInstancia(String objName, Class<T> expectedType) {
        String key = normalizeKey(objName);
        INSTANCIAS.remove(key);
        return getInstancia(key, expectedType);
    }

    public static Object reloadInstancia(String objName) {
        return reloadInstancia(objName, Object.class);
    }

    private static Object createInstance(String key) {
        ResourceBundle resourceBundle = loadResourceBundle();
        if (!resourceBundle.containsKey(key)) {
            throw new IllegalArgumentException(
                    "La clave '" + key + "' no existe en " + FACTORY_BUNDLE_NAME + ".properties.");
        }

        String className = resourceBundle.getString(key);
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La clave '" + key + "' en " + FACTORY_BUNDLE_NAME + ".properties no define una clase válida.");
        }

        String trimmedClassName = className.trim();
        try{
            Class<?> clazz = Class.forName(trimmedClassName);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("No se encontró la clase '" + trimmedClassName
                    + "' configurada para la clave '" + key + "'.", ex);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "No se pudo crear una instancia de la clase '" + trimmedClassName + "' para la clave '"
                            + key + "'.",
                    ex);
            }
        }



    private static ResourceBundle loadResourceBundle() {
        try {
            return ResourceBundle.getBundle(FACTORY_BUNDLE_NAME);
        } catch (MissingResourceException ex) {
            throw new IllegalStateException(
                    "No se encontró el archivo " + FACTORY_BUNDLE_NAME + ".properties en el classpath.", ex);
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
