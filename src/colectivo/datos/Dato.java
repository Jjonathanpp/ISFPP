package colectivo.datos;

import colectivo.modelo.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

public class Dato {

    private static TreeMap<String, Linea> lineas;
    private static TreeMap<String, Frecuencia> frecuencias;
    private static TreeMap<String, Parada> paradas;
    private static TreeMap<String, Tramo> tramos;

    public static TreeMap<String, Parada> cargarParadas(String nombreArchivo) throws FileNotFoundException {
        paradas = new TreeMap<>();

        InputStream input = Dato.class.getClassLoader().getResourceAsStream(nombreArchivo);
        if (input == null)
            throw new FileNotFoundException("No se encontró el archivo en resources: " + nombreArchivo);

        Scanner read = new Scanner(input, "UTF-8");
        int contador = 0;

        while (read.hasNextLine()) {
            String lineaTxt = read.nextLine().trim();
            if (lineaTxt.isEmpty()) continue;

            String[] partes = lineaTxt.split("\\s*;\\s*");
            if (partes.length < 4) continue;

            String codigo = partes[0];
            String direccion = partes[1];
            double latitud = Double.parseDouble(partes[2]);
            double longitud = Double.parseDouble(partes[3]);

            paradas.put(codigo, new Parada(codigo, direccion, latitud, longitud));
            contador++;
        }

        read.close();
        System.out.println("Paradas cargadas: " + contador);
        return paradas;
    }

    public static TreeMap<String, Linea> cargarLineas(String nombreArchivo) throws FileNotFoundException {
        lineas = new TreeMap<>();
        Scanner read = new Scanner(new File(nombreArchivo));

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

                Parada parada = paradas.get(codigoParada);
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
            }
        }
        read.close();
        return lineas;
    }

    public static TreeMap<String, Tramo> cargarTramos(String nombreArchivo) throws FileNotFoundException {
        tramos = new TreeMap<>();
        Scanner read = new Scanner(new File(nombreArchivo));
        read.useDelimiter("\\s*;\\s*");
        int tiempo, tipo;
        Parada inicio, fin;
        while(read.hasNext()) {
            tiempo = Integer.parseInt(read.next());
            tipo = Integer.parseInt(read.next());
            String codigoInicio = read.next();
            String codigoFin = read.next();

            inicio = paradas.get(codigoInicio);
            fin = paradas.get(codigoFin);

            if(inicio != null && fin != null) {
                Tramo tramo = new Tramo(tiempo, tipo, inicio, fin);
                tramos.put(codigoInicio + "-" + codigoFin, tramo);
            }
        }
        return tramos;
    }

    public static TreeMap<String, Frecuencia> cargarFrecuencias(String nombreArchivo) throws FileNotFoundException {
        frecuencias = new TreeMap<>();
        Scanner read = new Scanner(new File(nombreArchivo));
        read.useDelimiter("\\s*;\\s*");

        while(read.hasNext()) {
            String lineaTxt = read.nextLine().trim();
            if (lineaTxt.isEmpty()) continue; // salta líneas vacías

            String[] partes = lineaTxt.split("\\s*;\\s*");
            if (partes.length < 3) continue; // salta líneas incompletas

            String idLinea = partes[0];
            int diaSemana = Integer.parseInt(partes[1]);
            LocalTime hora = LocalTime.parse(partes[2]);

            Linea linea = lineas.get(idLinea);
            if (linea != null) {
                Frecuencia frecuencia = new Frecuencia(linea, diaSemana, hora);
                frecuencias.put(idLinea + "-" + diaSemana + "-" + hora, frecuencia);
            }
        }
        read.close();
        return frecuencias;
    }
}
