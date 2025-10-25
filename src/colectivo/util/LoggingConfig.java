package colectivo.util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inicializa Log4j leyendo el archivo de propiedades que vive en el classpath.
 * La configuración crea tanto una salida por consola como un archivo dedicado
 * a los errores para ser consumido por el visor de logs de la aplicación.
 */
public final class LoggingConfig {

    private static final Logger LOGGER = Logger.getLogger(LoggingConfig.class);
    private static final String CONFIG_FILE = "log4j.properties";
    private static boolean initialized;

    private LoggingConfig() {
        throw new IllegalStateException("No se debe instanciar LoggingConfig");
    }

    public static synchronized void initLogging() {
        if (initialized) {
            return;
        }

        try {
            createLogDirectory();

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL configUrl = classLoader.getResource(CONFIG_FILE);

            if (configUrl != null) {
                PropertyConfigurator.configure(configUrl);
                LOGGER.info("Log4j configurado usando " + configUrl);
            } else {
                BasicConfigurator.configure();
                LOGGER.warn("No se encontró " + CONFIG_FILE + " en el classpath. Se aplicó BasicConfigurator por defecto.");
            }
            initialized = true;
        } catch (Exception ex) {
            BasicConfigurator.configure();
            LOGGER.error("No se pudo inicializar Log4j a partir de " + CONFIG_FILE, ex);
        }
    }

    private static void createLogDirectory() {
        Path logDir = Paths.get("logs");
        try {
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear el directorio de logs en " + logDir.toAbsolutePath(), e);
        }
    }
}