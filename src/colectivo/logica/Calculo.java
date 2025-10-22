package colectivo.logica;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.*;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Clase que implementa la lógica de cálculo de recorridos entre paradas.
 * Soporta rutas directas, con transbordo y con conexión caminando.
 */
public class Calculo {

    // Método principal (API pública)
    public static List<List<Recorrido>> calcularRecorrido(
            Parada paradaOrigen,
            Parada paradaDestino,
            int diaSemana,
            LocalTime horaLlegadaParada,
            Map<String, Tramo> tramos) {

        DatosRed datos = cargarDatos(tramos);
        if (datos == null) return Collections.emptyList();

        Parada origen = datos.paradasPorCodigo().get(paradaOrigen.getCodigo());
        Parada destino = datos.paradasPorCodigo().get(paradaDestino.getCodigo());

        if (origen == null || destino == null || origen.equals(destino))
            return Collections.emptyList();

        return calcularRecorridosInternos(origen, destino, diaSemana, horaLlegadaParada, datos.tramos());
    }

    // ==========================================================
    // ================ LÓGICA PRINCIPAL ========================
    // ==========================================================

    private static List<List<Recorrido>> calcularRecorridosInternos(
            Parada origen, Parada destino, int diaSemana, LocalTime hora, Map<String, Tramo> tramos) {

        // 1. Buscar rutas directas
        List<List<Recorrido>> rutas = buscarRutasDirectas(origen, destino, diaSemana, hora, tramos);
        if (!rutas.isEmpty()) return rutas;

        // 2. Buscar rutas con transbordo
        rutas = buscarRutasConTransbordo(origen, destino, diaSemana, hora, tramos);
        if (!rutas.isEmpty()) return rutas;

        // 3. Buscar rutas con conexión caminando
        return buscarRutasConexionCaminando(origen, destino, diaSemana, hora, tramos);
    }

    // ==========================================================
    // ================ MÉTODOS DE CARGA DE DATOS ===============
    // ==========================================================

    private static DatosRed cargarDatos(Map<String, Tramo> tramosDestino) {
        try {
            ParadaDAO paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
            LineaDAO lineaDAO = (LineaDAO) Factory.getInstancia("LINEA");
            TramoDAO tramoDAO = (TramoDAO) Factory.getInstancia("TRAMO");

            Map<Integer, Parada> paradasPorId = paradaDAO.buscarTodos();
            Map<String, Parada> paradasPorCodigo = paradasPorId.values().stream()
                    .collect(Collectors.toMap(Parada::getCodigo, Function.identity()));

            Map<String, Linea> lineas = lineaDAO.buscarTodos();
            conectarLineasYParadas(lineas, paradasPorCodigo);

            Map<String, Tramo> tramos = cargarTramos(tramoDAO, tramosDestino, paradasPorCodigo);
            return new DatosRed(paradasPorCodigo, tramos);

        } catch (Exception e) {
            System.err.println("❌ Error cargando datos desde Factory: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void conectarLineasYParadas(Map<String, Linea> lineas, Map<String, Parada> paradas) {
        for (Linea linea : lineas.values()) {
            List<Parada> reemplazadas = linea.getParadas().stream()
                    .map(p -> paradas.get(p.getCodigo()))
                    .filter(Objects::nonNull)
                    .toList();

            linea.setParadas(new ArrayList<>(reemplazadas));
            reemplazadas.forEach(p -> p.addLinea(linea));
        }
    }

    private static Map<String, Tramo> cargarTramos(TramoDAO tramoDAO, Map<String, Tramo> tramosDestino, Map<String, Parada> paradas) {
        Map<String, Tramo> tramosDesdeDAO = tramoDAO.buscarTodos();
        Map<String, Tramo> tramos = (tramosDestino != null) ? tramosDestino : new HashMap<>();
        if (tramosDestino != null) tramosDestino.clear();

        for (var entry : tramosDesdeDAO.entrySet()) {
            Tramo t = entry.getValue();
            Parada inicio = paradas.get(t.getInicio().getCodigo());
            Parada fin = paradas.get(t.getFin().getCodigo());
            if (inicio == null || fin == null) continue;

            t.setInicio(inicio);
            t.setFin(fin);
            tramos.put(entry.getKey(), t);

            if (t.getTipo() == 2) { // conexión caminando
                inicio.addParadaCaminando(fin);
                fin.addParadaCaminando(inicio);
            }
        }
        return tramos;
    }

    private record DatosRed(Map<String, Parada> paradasPorCodigo, Map<String, Tramo> tramos) {}


    // ==========================================================
    // ================ RUTAS: DIRECTAS / TRANSBORDO / CAMINAR ==
    // ==========================================================

    private static List<List<Recorrido>> buscarRutasDirectas(
            Parada origen, Parada destino, int dia, LocalTime hora, Map<String, Tramo> tramos) {

        List<List<Recorrido>> resultados = new ArrayList<>();

        for (Linea linea : origen.getLineas()) {
            if (!destino.getLineas().contains(linea)) continue;

            List<Parada> paradas = linea.getParadas();
            int iOrigen = buscarIndice(paradas, origen);
            int iDestino = buscarIndice(paradas, destino);
            if (iOrigen == -1 || iDestino == -1 || iOrigen >= iDestino) continue;

            List<Parada> tramo = paradas.subList(iOrigen, iDestino + 1);
            int duracion = calcularDuracion(tramo, tramos);
            LocalTime salida = obtenerHorario(linea, dia, hora, origen, tramos);
            if (salida == null) continue;

            resultados.add(List.of(crearRecorrido(salida, linea, tramo, duracion)));
        }

        return resultados;
    }

    private static List<List<Recorrido>> buscarRutasConTransbordo(
            Parada origen, Parada destino, int dia, LocalTime hora, Map<String, Tramo> tramos) {

        Map<String, List<Recorrido>> mejores = new HashMap<>();

        for (Linea l1 : origen.getLineas()) {
            int iOrigen = buscarIndice(l1.getParadas(), origen);
            if (iOrigen == -1) continue;

            for (int i = iOrigen + 1; i < l1.getParadas().size(); i++) {
                Parada transbordo = l1.getParadas().get(i);

                for (Linea l2 : transbordo.getLineas()) {
                    if (l1.equals(l2)) continue;

                    int iT = buscarIndice(l2.getParadas(), transbordo);
                    int iDest = buscarIndice(l2.getParadas(), destino);
                    if (iT == -1 || iDest == -1 || iT >= iDest) continue;

                    List<Parada> tramo1 = l1.getParadas().subList(iOrigen, i + 1);
                    List<Parada> tramo2 = l2.getParadas().subList(iT, iDest + 1);

                    LocalTime salida1 = obtenerHorario(l1, dia, hora, origen, tramos);
                    if (salida1 == null) continue;

                    LocalTime llegadaT = salida1.plusSeconds(calcularDuracion(tramo1, tramos));
                    LocalTime salida2 = obtenerHorario(l2, dia, llegadaT, transbordo, tramos);
                    if (salida2 == null) continue;

                    List<Recorrido> ruta = List.of(
                            crearRecorrido(salida1, l1, tramo1, calcularDuracion(tramo1, tramos)),
                            crearRecorrido(salida2, l2, tramo2, calcularDuracion(tramo2, tramos))
                    );

                    mejores.putIfAbsent(l1.getCodigo() + "-" + l2.getCodigo(), ruta);
                }
            }
        }

        return new ArrayList<>(mejores.values());
    }

    private static List<List<Recorrido>> buscarRutasConexionCaminando(
            Parada origen, Parada destino, int dia, LocalTime hora, Map<String, Tramo> tramos) {

        List<List<Recorrido>> resultados = new ArrayList<>();

        for (Linea l1 : origen.getLineas()) {
            int iOrigen = buscarIndice(l1.getParadas(), origen);
            if (iOrigen == -1) continue;

            for (int i = iOrigen + 1; i < l1.getParadas().size(); i++) {
                Parada p1 = l1.getParadas().get(i);

                for (Parada pCaminar : p1.getParadasCaminando()) {
                    for (Linea l2 : pCaminar.getLineas()) {
                        if (l1.equals(l2)) continue;

                        int iCam = buscarIndice(l2.getParadas(), pCaminar);
                        int iDest = buscarIndice(l2.getParadas(), destino);
                        if (iCam == -1 || iDest == -1 || iCam >= iDest) continue;

                        List<Parada> tramo1 = l1.getParadas().subList(iOrigen, i + 1);
                        List<Parada> caminata = List.of(p1, pCaminar);
                        List<Parada> tramo2 = l2.getParadas().subList(iCam, iDest + 1);

                        LocalTime salida1 = obtenerHorario(l1, dia, hora, origen, tramos);
                        if (salida1 == null) continue;

                        LocalTime llegadaP1 = salida1.plusSeconds(calcularDuracion(tramo1, tramos));
                        LocalTime salidaCaminar = llegadaP1;
                        LocalTime llegadaCaminando = salidaCaminar.plusSeconds(calcularDuracion(caminata, tramos));

                        LocalTime salida2 = obtenerHorario(l2, dia, llegadaCaminando, pCaminar, tramos);
                        if (salida2 == null) continue;

                        List<Recorrido> ruta = List.of(
                                crearRecorrido(salida1, l1, tramo1, calcularDuracion(tramo1, tramos)),
                                crearRecorrido(salidaCaminar, null, caminata, calcularDuracion(caminata, tramos)),
                                crearRecorrido(salida2, l2, tramo2, calcularDuracion(tramo2, tramos))
                        );

                        resultados.add(ruta);
                    }
                }
            }
        }
        return resultados;
    }

    // ==========================================================
    // ================ UTILITARIOS =============================
    // ==========================================================

    private static int buscarIndice(List<Parada> paradas, Parada parada) {
        for (int i = 0; i < paradas.size(); i++)
            if (paradas.get(i).getCodigo().equals(parada.getCodigo()))
                return i;
        return -1;
    }

    private static int calcularDuracion(List<Parada> paradas, Map<String, Tramo> tramos) {
        int total = 0;
        for (int i = 0; i < paradas.size() - 1; i++) {
            Tramo t = tramos.get(paradas.get(i).getCodigo() + "-" + paradas.get(i + 1).getCodigo());
            if (t != null) total += t.getTiempo();
        }
        return total;
    }

    private static LocalTime obtenerHorario(Linea linea, int dia, LocalTime horaMinima, Parada inicio, Map<String, Tramo> tramos) {
        if (linea.getFrecuencias() == null || linea.getFrecuencias().isEmpty()) return null;
        int offset = calcularDuracionHastaParada(linea, inicio, tramos);

        return linea.getFrecuencias().stream()
                .filter(f -> f.getDiaSemana() == dia)
                .map(Frecuencia::getHora)
                .sorted()
                .map(h -> h.plusSeconds(offset))
                .filter(h -> h.isAfter(horaMinima))
                .findFirst()
                .orElse(null);
    }

    private static int calcularDuracionHastaParada(Linea linea, Parada paradaInicio, Map<String, Tramo> tramos) {
        int idx = buscarIndice(linea.getParadas(), paradaInicio);
        if (idx <= 0) return 0;
        return calcularDuracion(linea.getParadas().subList(0, idx + 1), tramos);
    }

    private static Recorrido crearRecorrido(LocalTime horaSalida, Linea linea, List<Parada> paradas, int duracion) {
        Recorrido r = new Recorrido(horaSalida, linea, paradas.get(0), paradas.get(1));
        for (int i = 2; i < paradas.size(); i++) r.agregarParada(paradas.get(i));
        r.setDuracion(duracion);
        return r;
    }
}
