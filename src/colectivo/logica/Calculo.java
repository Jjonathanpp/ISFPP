package colectivo.logica;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.dao.secuencial.TramoSecuencialDAO;
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

    //LISTO
    public static List<List<Recorrido>> calcularRecorrido(Parada paradaOrigen, Parada paradaDestino,
                                                          int diaSemana, LocalTime horaLlegadaParada,
                                                          Map<String, Tramo> tramos) {
        DatosRed datos = cargarDatosDesdeBD(tramos);

        if (datos == null) {
            return Collections.emptyList();
        }

        Parada origen = datos.paradasPorCodigo().get(paradaOrigen.getCodigo());
        Parada destino = datos.paradasPorCodigo().get(paradaDestino.getCodigo());

        if (origen == null || destino == null) {
            return Collections.emptyList();
        }

        return calcularRecorridoDesdeDatos(origen, destino, diaSemana, horaLlegadaParada, datos.tramos());
    }

    private static List<List<Recorrido>> calcularRecorridoDesdeDatos(Parada paradaOrigen, Parada paradaDestino,
                                                                     int diaSemana, LocalTime horaLlegadaParada,
                                                                     Map<String, Tramo> tramos) {

        List<List<Recorrido>> todosLosRecorridos = new ArrayList<>();

        // 1. Buscar rutas directas (una sola línea)
        List<List<Recorrido>> rutasDirectas = buscarRutasDirectas(paradaOrigen, paradaDestino,
                diaSemana, horaLlegadaParada, tramos);
        todosLosRecorridos.addAll(rutasDirectas);

        // Si hay rutas directas, no buscar otras más complejas
        if (!rutasDirectas.isEmpty()) {
            return todosLosRecorridos;
        }

        // 2. Buscar rutas con transbordo en la misma parada (dos líneas)
        List<List<Recorrido>> rutasConTransbordo = buscarRutasConTransbordo(paradaOrigen, paradaDestino,
                diaSemana, horaLlegadaParada, tramos);
        todosLosRecorridos.addAll(rutasConTransbordo);

        // Si hay rutas con transbordo, no buscar con conexión caminando
        if (!rutasConTransbordo.isEmpty()) {
            return todosLosRecorridos;
        }

        // 3. Buscar rutas con conexión caminando (dos líneas + tramo a pie)
        List<List<Recorrido>> rutasConexionCaminando = buscarRutasConexionCaminando(paradaOrigen,
                paradaDestino, diaSemana, horaLlegadaParada, tramos);
        todosLosRecorridos.addAll(rutasConexionCaminando);

        return todosLosRecorridos;
    }
    private static DatosRed cargarDatosDesdeBD(Map<String, Tramo> tramosDestino) {
        try {
            ParadaDAO paradaDAO = new ParadaSecuencialDAO();
            LineaDAO lineaDAO = new LineaSecuencialDAO();
            TramoDAO tramoDAO = new TramoSecuencialDAO();

            Map<Integer, Parada> paradasPorId = paradaDAO.buscarTodos();
            Map<String, Parada> paradasPorCodigo = paradasPorId.values().stream()
                    .collect(Collectors.toMap(Parada::getCodigo, Function.identity()));

            Map<String, Linea> lineas = lineaDAO.buscarTodos();

            for (Linea linea : lineas.values()) {
                List<Parada> paradasReemplazadas = linea.getParadas().stream()
                        .map(parada -> paradasPorCodigo.get(parada.getCodigo()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                linea.setParadas(paradasReemplazadas);
                for (Parada parada : paradasReemplazadas) {
                    if (parada != null) {
                        parada.addLinea(linea);
                    }
                }
            }

            Map<String, Tramo> tramosDesdeDAO = tramoDAO.buscarTodos();
            Map<String, Tramo> tramos = tramosDestino != null ? tramosDestino : new HashMap<>();

            if (tramosDestino != null) {
                tramosDestino.clear();
            }

            for (Map.Entry<String, Tramo> entry : tramosDesdeDAO.entrySet()) {
                Tramo tramo = entry.getValue();
                Parada inicio = paradasPorCodigo.get(tramo.getInicio().getCodigo());
                Parada fin = paradasPorCodigo.get(tramo.getFin().getCodigo());

                if (inicio != null && fin != null) {
                    tramo.setInicio(inicio);
                    tramo.setFin(fin);
                    tramos.put(entry.getKey(), tramo);

                    if (tramo.getTipo() == 2) {
                        if (inicio.getParadasCaminando().stream().noneMatch(p -> p.getCodigo().equals(fin.getCodigo()))) {
                            inicio.addParadaCaminando(fin);
                        }
                        if (fin.getParadasCaminando().stream().noneMatch(p -> p.getCodigo().equals(inicio.getCodigo()))) {
                            fin.addParadaCaminando(inicio);
                        }
                    }
                }
            }

            return new DatosRed(paradasPorCodigo, tramos);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private record DatosRed(Map<String, Parada> paradasPorCodigo, Map<String, Tramo> tramos) {
    }

    //Listo
    private static List<List<Recorrido>> buscarRutasDirectas(Parada origen, Parada destino, int diaSemana,
                                                             LocalTime horaLlegada, Map<String, Tramo> tramos) {
        List<List<Recorrido>> resultados = new ArrayList<>();

        // Buscar líneas que pasen por ambas paradas
        for (Linea linea : origen.getLineas()) {
            if (destino.getLineas().contains(linea)) {
                List<Parada> paradasLinea = linea.getParadas();
                int indiceOrigen = buscarIndiceParada(paradasLinea, origen);
                int indiceDestino = buscarIndiceParada(paradasLinea, destino);

                if (indiceOrigen != -1 && indiceDestino != -1 && indiceOrigen < indiceDestino) {
                    List<Parada> paradasRecorrido = paradasLinea.subList(indiceOrigen, indiceDestino + 1);
                    int duracion = calcularDuracionTramo(paradasRecorrido, tramos);

                    // Obtener primer horario desde la parada de origen (con offset)
                    LocalTime horaSalida = obtenerPrimerHorarioDisponible(linea, diaSemana, horaLlegada, origen, tramos);

                    if (horaSalida != null) {
                        Recorrido recorrido = crearRecorrido(horaSalida, linea, paradasRecorrido, duracion);
                        List<Recorrido> ruta = new ArrayList<>();
                        ruta.add(recorrido);
                        resultados.add(ruta);
                    }
                }
            }
        }

        return resultados;
    }

    //LISTO
    private static List<List<Recorrido>> buscarRutasConTransbordo(Parada origen, Parada destino,
                                                                  int diaSemana, LocalTime horaLlegada,
                                                                  Map<String, Tramo> tramos) {
        List<List<Recorrido>> resultados = new ArrayList<>();
        Map<String, List<Recorrido>> mejoresRutasPorLinea = new HashMap<>();

        // Por cada línea que pasa por origen
        for (Linea linea1 : origen.getLineas()) {
            List<Parada> paradas1 = linea1.getParadas();
            int indiceOrigen = buscarIndiceParada(paradas1, origen);

            if (indiceOrigen == -1) continue;

            // Explorar cada parada posterior como posible transbordo
            for (int i = indiceOrigen + 1; i < paradas1.size(); i++) {
                Parada paradaTransbordo = paradas1.get(i);

                // Ver si alguna línea de esta parada llega al destino
                for (Linea linea2 : paradaTransbordo.getLineas()) {
                    if (linea1.equals(linea2)) continue;

                    List<Parada> paradas2 = linea2.getParadas();
                    int indiceTransbordo = buscarIndiceParada(paradas2, paradaTransbordo);
                    int indiceDestino = buscarIndiceParada(paradas2, destino);

                    if (indiceTransbordo != -1 && indiceDestino != -1 && indiceTransbordo < indiceDestino) {
                        List<Parada> paradasRecorrido1 = paradas1.subList(indiceOrigen, i + 1);
                        List<Parada> paradasRecorrido2 = paradas2.subList(indiceTransbordo, indiceDestino + 1);

                        int duracion1 = calcularDuracionTramo(paradasRecorrido1, tramos);
                        int duracion2 = calcularDuracionTramo(paradasRecorrido2, tramos);

                        // Primer horario desde origen en linea1 (con offset)
                        LocalTime horaSalida1 = obtenerPrimerHorarioDisponible(linea1, diaSemana, horaLlegada, origen, tramos);

                        if (horaSalida1 != null) {
                            LocalTime horaLlegadaTransbordo = horaSalida1.plusSeconds(duracion1);
                            // Primer horario desde transbordo en linea2 (con offset, hora minima = llegada a transbordo)
                            LocalTime horaSalida2 = obtenerPrimerHorarioDisponible(linea2, diaSemana, horaLlegadaTransbordo, paradaTransbordo, tramos);

                            if (horaSalida2 != null) {
                                String clave = linea1.getCodigo() + "-" + linea2.getCodigo();

                                List<Recorrido> ruta = new ArrayList<>();
                                ruta.add(crearRecorrido(horaSalida1, linea1, paradasRecorrido1, duracion1));
                                ruta.add(crearRecorrido(horaSalida2, linea2, paradasRecorrido2, duracion2));

                                // Guardar solo si es la primera encontrada para este par de líneas
                                if (!mejoresRutasPorLinea.containsKey(clave)) {
                                    mejoresRutasPorLinea.put(clave, ruta);
                                }
                            }
                        }
                    }
                }
            }
        }

        resultados.addAll(mejoresRutasPorLinea.values());
        return resultados;
    }


    private static List<List<Recorrido>> buscarRutasConexionCaminando(Parada origen, Parada destino,
                                                                      int diaSemana, LocalTime horaLlegada,
                                                                      Map<String, Tramo> tramos) {
        List<List<Recorrido>> resultados = new ArrayList<>();

        // Por cada línea que pasa por origen
        for (Linea linea1 : origen.getLineas()) {
            List<Parada> paradas1 = linea1.getParadas();
            int indiceOrigen = buscarIndiceParada(paradas1, origen);

            if (indiceOrigen == -1) continue;

            // Explorar cada parada posterior
            for (int i = indiceOrigen + 1; i < paradas1.size(); i++) {
                Parada parada1 = paradas1.get(i);

                // Ver si desde esta parada se puede caminar a otra
                for (Parada paradaCaminando : parada1.getParadasCaminando()) {
                    // Ver si desde la parada caminando hay línea al destino
                    for (Linea linea2 : paradaCaminando.getLineas()) {
                        if (linea1.equals(linea2)) continue;

                        List<Parada> paradas2 = linea2.getParadas();
                        int indiceCaminando = buscarIndiceParada(paradas2, paradaCaminando);
                        int indiceDestino = buscarIndiceParada(paradas2, destino);

                        if (indiceCaminando != -1 && indiceDestino != -1 && indiceCaminando < indiceDestino) {
                            List<Parada> paradasRecorrido1 = paradas1.subList(indiceOrigen, i + 1);
                            List<Parada> paradasCaminando = Arrays.asList(parada1, paradaCaminando);
                            List<Parada> paradasRecorrido2 = paradas2.subList(indiceCaminando, indiceDestino + 1);

                            int duracion1 = calcularDuracionTramo(paradasRecorrido1, tramos);
                            int duracionCaminar = calcularDuracionTramo(paradasCaminando, tramos);
                            int duracion2 = calcularDuracionTramo(paradasRecorrido2, tramos);

                            // Primer horario desde origen en linea1 (con offset)
                            LocalTime horaSalida1 = obtenerPrimerHorarioDisponible(linea1, diaSemana, horaLlegada, origen, tramos);

                            if (horaSalida1 != null) {
                                LocalTime horaLlegadaParada1 = horaSalida1.plusSeconds(duracion1);
                                LocalTime horaSalidaCaminar = horaLlegadaParada1;  // Inicio de caminata = llegada del bus
                                LocalTime horaLlegadaCaminando = horaSalidaCaminar.plusSeconds(duracionCaminar);

                                // Primer horario desde paradaCaminando en linea2 (con offset)
                                LocalTime horaSalida2 = obtenerPrimerHorarioDisponible(linea2, diaSemana, horaLlegadaCaminando, paradaCaminando, tramos);

                                if (horaSalida2 != null) {
                                    List<Recorrido> ruta = new ArrayList<>();
                                    ruta.add(crearRecorrido(horaSalida1, linea1, paradasRecorrido1, duracion1));
                                    ruta.add(crearRecorrido(horaSalidaCaminar, null, paradasCaminando, duracionCaminar));  // Caminata: linea=null
                                    ruta.add(crearRecorrido(horaSalida2, linea2, paradasRecorrido2, duracion2));
                                    resultados.add(ruta);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Buscar todas las posibles rutas
        return resultados;
    }

    // Calcula la duración (offset) desde la primera parada de la línea hasta la paradaInicio
    //Listo
    private static int calcularDuracionHastaParada(Linea linea, Parada paradaInicio, Map<String, Tramo> tramos) {
        List<Parada> paradasLinea = linea.getParadas();
        int indiceInicio = buscarIndiceParada(paradasLinea, paradaInicio);
        if (indiceInicio == 0) {
            return 0;  // Si es la primera parada, offset = 0
        }
        // SubLista desde inicio de línea hasta paradaInicio (incluyendo)
        List<Parada> paradasHastaInicio = paradasLinea.subList(0, indiceInicio + 1);
        return calcularDuracionTramo(paradasHastaInicio, tramos);
    }

    // Obtiene el primer horario de salida DESDE la paradaInicio después de horaMinima (ajustado por offset)
    private static LocalTime obtenerPrimerHorarioDisponible(Linea linea, int diaSemana, LocalTime horaMinima,
                                                            Parada paradaInicio, Map<String, Tramo> tramos) {
        if (linea.getFrecuencias() == null || linea.getFrecuencias().isEmpty()) {
            System.err.println("Línea " + linea.getCodigo() + " no tiene frecuencias");
            return null;
        }

        int offset = calcularDuracionHastaParada(linea, paradaInicio, tramos);

        // Obtener todos los horarios base (salida desde inicio de línea) para el día, ordenados
        List<LocalTime> horariosBase = linea.getFrecuencias().stream()
                .filter(f -> f.getDiaSemana() == diaSemana)
                .map(Frecuencia::getHora)
                .sorted()
                .collect(Collectors.toList());

        LocalTime primerHorario = null;
        for (LocalTime horarioBase : horariosBase) {
            // Horario de salida efectiva desde la paradaInicio = base + offset
            LocalTime horaSalidaDesdeParada = horarioBase.plusSeconds(offset);
            // Debe ser estrictamente después de la hora mínima
            if (horaSalidaDesdeParada.isAfter(horaMinima)) {
                if (primerHorario == null || horaSalidaDesdeParada.isBefore(primerHorario)) {
                    primerHorario = horaSalidaDesdeParada;
                }
            }
        }

        return primerHorario;
    }

    //Listo
    private static int buscarIndiceParada(List<Parada> paradas, Parada parada) {
        for (int i = 0; i < paradas.size(); i++) {
            if (paradas.get(i).getCodigo().equals(parada.getCodigo())) {
                return i;
            }
        }
        return -1;
    }

    //LIsto
    private static int calcularDuracionTramo(List<Parada> paradas, Map<String, Tramo> tramos) {
        int duracionTotal = 0;

        for (int i = 0; i < paradas.size() - 1; i++) {
            String claveTramo = paradas.get(i).getCodigo() + "-" + paradas.get(i + 1).getCodigo();
            Tramo tramo = tramos.get(claveTramo);

            if (tramo != null) {
                duracionTotal += tramo.getTiempo();
            }
        }

        return duracionTotal;
    }

    //Listo
    private static Recorrido crearRecorrido(LocalTime horaSalida, Linea linea, List<Parada> paradas, int duracion) {
        if (paradas.size() < 2) {
            throw new IllegalArgumentException("El recorrido debe tener al menos dos paradas");
        }

        Recorrido recorrido = new Recorrido(horaSalida, linea, paradas.get(0), paradas.get(1));

        for (int i = 2; i < paradas.size(); i++) {
            recorrido.agregarParada(paradas.get(i));
        }

        // Asignar duración al recorrido
        recorrido.setDuracion(duracion);

        return recorrido;
    }
}
