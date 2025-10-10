package colectivo.datos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CargaParametros {
    private static String archivoFrecuencia;
    private static String archivoLinea;
    private static String archivoParada;
    private static String archivoTramo;

    public static void parametros() throws IOException {
        Properties prop = new Properties();
        InputStream input = CargaParametros.class.getClassLoader().getResourceAsStream("config.properties");

        prop.load(input);

        archivoFrecuencia = prop.getProperty("frecuencia");
        archivoLinea = prop.getProperty("linea");
        archivoParada = prop.getProperty("parada");
        archivoTramo = prop.getProperty("tramo");
    }

    public static String getArchivoFrecuencia() { return archivoFrecuencia; }
    public static String getArchivoLinea() { return archivoLinea; }
    public static String getArchivoParada() { return archivoParada; }
    public static String getArchivoTramo() { return archivoTramo; }
}
