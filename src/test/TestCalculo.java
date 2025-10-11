package test;

import colectivo.logica.Calculo;
import colectivo.modelo.*;
import colectivo.dao.secuencial.*;
import java.time.LocalTime;
import java.util.*;


public class TestCalculo {

    public static void main(String[] args) {
        // Crear instancia de Calculo
        Calculo calculo = new Calculo();

        // Cargar datos desde la base de datos
        System.out.println("Cargando datos desde la base de datos...\n");

        LineaSecuencialDAO lineaDAO = new LineaSecuencialDAO();
        Map<String, Linea> lineas = lineaDAO.buscarTodos();

        TramoSecuencialDAO tramoDAO = new TramoSecuencialDAO();
        Map<String, Tramo> tramos = tramoDAO.buscarTodos();

        ParadaSecuencialDAO paradaDAO = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = paradaDAO.buscarTodos();

        System.out.println("Datos cargados:");
        System.out.println("- Líneas: " + lineas.size());
        System.out.println("- Tramos: " + tramos.size());
        System.out.println("- Paradas: " + paradas.size());
        System.out.println();

        // ============================================
        // TEST 1: RUTA DIRECTA
        // ============================================
        System.out.println("========================================");
        System.out.println("TEST 1: RUTA DIRECTA (44 -> 47)");
        System.out.println("========================================\n");

        Parada p44 = paradas.get(44);
        Parada p47 = paradas.get(47);

        if (p44 != null && p47 != null) {
            LocalTime horaLlegada1 = LocalTime.of(10, 35);
            List<List<Recorrido>> resultados1 = calculo.calcularRecorrido(p44, p47, 1, horaLlegada1, tramos);

            if (resultados1.isEmpty()) {
                System.out.println("No se encontraron rutas disponibles.");
            } else {
                calculo.imprimirResultados(p44, p47, horaLlegada1, resultados1, tramos);
            }
        } else {
            System.out.println("ERROR: No se encontraron las paradas 44 o 47 en la base de datos.");
        }

        // ============================================
        // TEST 2: RUTA CON TRANSBORDO
        // ============================================
        System.out.println("\n========================================");
        System.out.println("TEST 2: RUTA CON TRANSBORDO (88 -> 13)");
        System.out.println("========================================\n");

        Parada p88 = paradas.get(88);
        Parada p13 = paradas.get(13);

        if (p88 != null && p13 != null) {
            LocalTime horaLlegada2 = LocalTime.of(10, 35);
            List<List<Recorrido>> resultados2 = calculo.calcularRecorrido(p88, p13, 1, horaLlegada2, tramos);

            if (resultados2.isEmpty()) {
                System.out.println("No se encontraron rutas disponibles.");
            } else {
                calculo.imprimirResultados(p88, p13, horaLlegada2, resultados2, tramos);
            }
        } else {
            System.out.println("ERROR: No se encontraron las paradas 88 o 13 en la base de datos.");
        }

        // ============================================
        // TEST 3: PRUEBA PERSONALIZADA
        // ============================================
        System.out.println("\n========================================");
        System.out.println("TEST 3: PRUEBA PERSONALIZADA");
        System.out.println("========================================");


        // Cambia estos valores para probar otras rutas
        int codigoOrigen = 5;
        int codigoDestino = 24;
        LocalTime horaLlegada3 = LocalTime.of(9, 0);
        int diaSemana = 1; // 1 = Lunes

        Parada origen = paradas.get(codigoOrigen);
        Parada destino = paradas.get(codigoDestino);

        if (origen != null && destino != null) {
            List<List<Recorrido>> resultados3 = calculo.calcularRecorrido(origen, destino, diaSemana, horaLlegada3, tramos);

            if (resultados3.isEmpty()) {
                System.out.println("No se encontraron rutas disponibles.");
            } else {
                calculo.imprimirResultados(origen, destino, horaLlegada3, resultados3, tramos);
            }
        } else {
            System.out.println("ERROR: No se encontraron las paradas " + codigoOrigen + " o " + codigoDestino + " en la base de datos.");
        }

        System.out.println("\n========================================");
        System.out.println("FIN DE LOS TESTS");
        System.out.println("========================================");
    }
}