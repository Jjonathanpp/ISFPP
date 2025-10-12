package colectivo.datos;

import colectivo.modelo.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.*;

public class CargarDatos {

    // Cambiado a Map<Integer, Parada> para coincidir con el test
    public static Map<Integer, Parada> cargarParadas(String nombreArchivo) throws FileNotFoundException {
        Map<Integer, Parada> paradas = new HashMap<>();

        InputStream input = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        if (input == null)
            throw new FileNotFoundException("No se encontró el archivo en resources: " + nombreArchivo);

        Scanner read = new Scanner(input, "UTF-8");

        while (read.hasNextLine()) {
            String lineaTxt = read.nextLine().trim();
            if (lineaTxt.isEmpty()) continue;

            String[] partes = lineaTxt.split("\\s*;\\s*");
            if (partes.length < 4) continue;

            String codigo = partes[0].trim();
            String direccion = partes[1].trim();
            double latitud = Double.parseDouble(partes[2].replace(',', '.'));
            double longitud = Double.parseDouble(partes[3].replace(',', '.'));

            Parada parada = new Parada(codigo, direccion, latitud, longitud);
            paradas.put(Integer.parseInt(codigo), parada);
        }

        read.close();
        return paradas;
    }

    // Modificado para recibir 3 parámetros como espera el test
    public static Map<String, Linea> cargarLineas(String archivoLinea, String archivoFrecuencia,
                                                  Map<Integer, Parada> paradas) throws FileNotFoundException {
        Map<String, Linea> lineas = new HashMap<>();

        // Primero cargar las líneas
        InputStream input = CargarDatos.class.getClassLoader().getResourceAsStream(archivoLinea);
        if (input == null)
            throw new FileNotFoundException("No se encontró el archivo en resources: " + archivoLinea);

        Scanner read = new Scanner(input, "UTF-8");

        while(read.hasNextLine()) {
            String lineaTxt = read.nextLine().trim();
            if(lineaTxt.isEmpty()) continue;
            String[] partes = lineaTxt.split("\\s*;\\s*");
            if(partes.length < 3) continue;

            String codigo = partes[0].trim();
            String nombre = partes[1].trim();

            List<Parada> paradasEnLaLinea = new ArrayList<>();
            boolean faltanParadas = false;
            for(int i = 2; i < partes.length; i++) {
                String codigoParada = partes[i].trim();
                if(codigoParada.isEmpty()) continue;

                Parada parada = paradas.get(Integer.parseInt(codigoParada));
                if(parada != null) {
                    paradasEnLaLinea.add(parada);
                } else {
                    faltanParadas = true;
                    break;
                }
            }
            if(!faltanParadas && paradasEnLaLinea.size() >= 2) {
                Linea linea = new Linea(codigo, nombre, paradasEnLaLinea);
                lineas.put(codigo, linea);

                // Agregar la línea a cada parada
                for(Parada parada : paradasEnLaLinea) {
                    parada.addLinea(linea);
                }
            }
        }
        read.close();

        // Ahora cargar las frecuencias
        cargarFrecuencias(archivoFrecuencia, lineas);

        return lineas;
    }

    // metodo auxiliar para cargar frecuencias
    private static void cargarFrecuencias(String nombreArchivo, Map<String, Linea> lineas)
            throws FileNotFoundException {
        InputStream input = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        if (input == null)
            throw new FileNotFoundException("No se encontró el archivo en resources: " + nombreArchivo);

        Scanner read = new Scanner(input, "UTF-8");

        while(read.hasNextLine()) {
            String lineaTxt = read.nextLine().trim();
            if (lineaTxt.isEmpty()) continue;

            String[] partes = lineaTxt.split("\\s*;\\s*");
            if (partes.length < 3) continue;

            String idLinea = partes[0].trim();  // AGREGAR .trim()
            int diaSemana = Integer.parseInt(partes[1].trim());  // AGREGAR .trim()
            LocalTime hora = LocalTime.parse(partes[2].trim());  // AGREGAR .trim()

            Linea linea = lineas.get(idLinea);
            if (linea != null) {
                Frecuencia frecuencia = new Frecuencia(linea, diaSemana, hora);
                linea.agregarFrecuencia(frecuencia);
            }
        }
        read.close();
    }


    // Modificado para recibir 2 parámetros como espera el test
    public static Map<String, Tramo> cargarTramos(String nombreArchivo, Map<Integer, Parada> paradas)
            throws FileNotFoundException {
        Map<String, Tramo> tramos = new HashMap<>();

        InputStream input = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        if (input == null)
            throw new FileNotFoundException("No se encontró el archivo en resources: " + nombreArchivo);

        Scanner read = new Scanner(input, "UTF-8");

        // Saltar la primera línea si es un encabezado
        boolean primeraLinea = true;

        while(read.hasNextLine()) {
            String lineaTxt = read.nextLine().trim();
            if (lineaTxt.isEmpty()) continue;

            // Saltar encabezado
            if (primeraLinea && lineaTxt.toLowerCase().contains("parada")) {
                primeraLinea = false;
                continue;
            }
            primeraLinea = false;

            // Split por punto y coma
            String[] partes = lineaTxt.split(";");
            if (partes.length < 4) {
                continue;
            }

            try {
                // Formato: Parada inicio; Parada fin; Tiempo recorrido; Tipo de recorrido;
                String codigoInicio = partes[0].trim();
                String codigoFin = partes[1].trim();
                int tiempo = Integer.parseInt(partes[2].trim());
                int tipo = Integer.parseInt(partes[3].trim());

                Parada inicio = paradas.get(Integer.parseInt(codigoInicio));
                Parada fin = paradas.get(Integer.parseInt(codigoFin));

                if(inicio != null && fin != null) {
                    Tramo tramo = new Tramo(tiempo, tipo, inicio, fin);
                    tramos.put(codigoInicio + "-" + codigoFin, tramo);

                    // Si es tipo 2 (caminando), agregar la relación bidireccional entre paradas
                    if(tipo == 2) {
                        inicio.addParadaCaminando(fin);
                        fin.addParadaCaminando(inicio);
                    }
                }
            } catch (NumberFormatException e) {
                // Ignorar líneas con formato incorrecto
            }
        }

        read.close();
        return tramos;
    }
}