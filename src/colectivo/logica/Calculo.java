package colectivo.logica;

import colectivo.modelo.*;
import colectivo.dao.secuencial.*;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Calculo {

    public List<List<Recorrido>> calcularRecorrido(Parada paradaOrigen, Parada paradaDestino, int diaSemana,
                                                   LocalTime horaLlegadaParada, Map<String, Tramo> tramos) {

        List<List<Recorrido>> todosLosRecorridos = new ArrayList<>();

        // Obtener todas las líneas
        LineaSecuencialDAO lineaDAO = new LineaSecuencialDAO();
        Map<String, Linea> lineas = lineaDAO.buscarTodos();

        // 1. Buscar rutas directas (una sola línea)
        List<List<Recorrido>> rutasDirectas = buscarRutasDirectas(paradaOrigen, paradaDestino,
                diaSemana, horaLlegadaParada,
                lineas, tramos);
        todosLosRecorridos.addAll(rutasDirectas);

        // 2. Buscar rutas con transbordo (dos líneas)
        List<List<Recorrido>> rutasConTransbordo = buscarRutasConTransbordo(paradaOrigen, paradaDestino,
                diaSemana, horaLlegadaParada,
                lineas, tramos);
        todosLosRecorridos.addAll(rutasConTransbordo);

        // 3. Ordenar por duración total
        todosLosRecorridos.sort((r1, r2) -> {
            int duracion1 = calcularDuracionTotal(r1, tramos);
            int duracion2 = calcularDuracionTotal(r2, tramos);
            return Integer.compare(duracion1, duracion2);
        });

        return todosLosRecorridos;
    }

    private List<List<Recorrido>> buscarRutasDirectas(Parada origen, Parada destino, int diaSemana,
                                                      LocalTime horaLlegada, Map<String, Linea> lineas,
                                                      Map<String, Tramo> tramos) {
        List<List<Recorrido>> resultados = new ArrayList<>();

        for (Linea linea : lineas.values()) {
            List<Parada> paradas = linea.getParadas();
            int indiceOrigen = buscarIndiceParada(paradas, origen);
            int indiceDestino = buscarIndiceParada(paradas, destino);

            // Verificar si ambas paradas están en la línea y en el orden correcto
            if (indiceOrigen != -1 && indiceDestino != -1 && indiceOrigen < indiceDestino) {
                // Obtener paradas del recorrido
                List<Parada> paradasRecorrido = paradas.subList(indiceOrigen, indiceDestino + 1);

                // Calcular duración del tramo
                int duracion = calcularDuracionTramo(paradasRecorrido, tramos);

                // Buscar horarios disponibles
                List<LocalTime> horarios = obtenerHorariosDisponibles(linea, diaSemana, horaLlegada);

                for (LocalTime horaSalida : horarios) {
                    List<Recorrido> recorrido = new ArrayList<>();
                    Recorrido r = crearRecorrido(horaSalida, linea, paradasRecorrido);
                    recorrido.add(r);
                    resultados.add(recorrido);
                }
            }
        }

        return resultados;
    }

    private List<List<Recorrido>> buscarRutasConTransbordo(Parada origen, Parada destino, int diaSemana,
                                                           LocalTime horaLlegada, Map<String, Linea> lineas,
                                                           Map<String, Tramo> tramos) {
        List<List<Recorrido>> resultados = new ArrayList<>();

        // Buscar paradas de transbordo
        for (Linea linea1 : lineas.values()) {
            List<Parada> paradas1 = linea1.getParadas();
            int indiceOrigen = buscarIndiceParada(paradas1, origen);

            if (indiceOrigen == -1) continue;

            // Para cada parada después del origen en linea1
            for (int i = indiceOrigen + 1; i < paradas1.size(); i++) {
                Parada paradaTransbordo = paradas1.get(i);

                // Buscar línea que conecte el transbordo con el destino
                for (Linea linea2 : lineas.values()) {
                    if (linea1.equals(linea2)) continue;

                    List<Parada> paradas2 = linea2.getParadas();
                    int indiceTransbordo = buscarIndiceParada(paradas2, paradaTransbordo);
                    int indiceDestino = buscarIndiceParada(paradas2, destino);

                    if (indiceTransbordo != -1 && indiceDestino != -1 && indiceTransbordo < indiceDestino) {
                        // Crear recorrido con transbordo
                        List<Parada> paradasRecorrido1 = paradas1.subList(indiceOrigen, i + 1);
                        List<Parada> paradasRecorrido2 = paradas2.subList(indiceTransbordo, indiceDestino + 1);

                        int duracion1 = calcularDuracionTramo(paradasRecorrido1, tramos);

                        // Buscar horarios
                        List<LocalTime> horarios1 = obtenerHorariosDisponibles(linea1, diaSemana, horaLlegada);

                        for (LocalTime horaSalida1 : horarios1) {
                            LocalTime horaLlegadaTransbordo = horaSalida1.plusMinutes(duracion1);

                            List<LocalTime> horarios2 = obtenerHorariosDisponibles(linea2, diaSemana, horaLlegadaTransbordo);

                            for (LocalTime horaSalida2 : horarios2) {
                                List<Recorrido> recorrido = new ArrayList<>();
                                recorrido.add(crearRecorrido(horaSalida1, linea1, paradasRecorrido1));
                                recorrido.add(crearRecorrido(horaSalida2, linea2, paradasRecorrido2));
                                resultados.add(recorrido);
                            }
                        }
                    }
                }
            }
        }

        return resultados;
    }

    private int buscarIndiceParada(List<Parada> paradas, Parada parada) {
        for (int i = 0; i < paradas.size(); i++) {
            if (paradas.get(i).getCodigo().equals(parada.getCodigo())) {
                return i;
            }
        }
        return -1;
    }

    private int calcularDuracionTramo(List<Parada> paradas, Map<String, Tramo> tramos) {
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

    private List<LocalTime> obtenerHorariosDisponibles(Linea linea, int diaSemana, LocalTime horaMinima) {
        List<LocalTime> horarios = new ArrayList<>();

        for (Frecuencia frecuencia : linea.getFrecuencias()) {
            if (frecuencia.getDiaSemana() == diaSemana &&
                    frecuencia.getHora().isAfter(horaMinima)) {
                horarios.add(frecuencia.getHora());
            }
        }

        // Ordenar horarios
        horarios.sort(LocalTime::compareTo);

        // Retornar solo los primeros 3 horarios disponibles
        return horarios.stream().limit(3).collect(Collectors.toList());
    }

    private Recorrido crearRecorrido(LocalTime horaSalida, Linea linea, List<Parada> paradas) {
        if (paradas.size() < 2) {
            throw new IllegalArgumentException("El recorrido debe tener al menos dos paradas");
        }

        Recorrido recorrido = new Recorrido(horaSalida, linea, paradas.get(0), paradas.get(1));

        // Agregar el resto de las paradas
        for (int i = 2; i < paradas.size(); i++) {
            recorrido.agregarParada(paradas.get(i));
        }

        return recorrido;
    }

    private int calcularDuracionTotal(List<Recorrido> recorridos, Map<String, Tramo> tramos) {
        int duracionTotal = 0;

        for (Recorrido recorrido : recorridos) {
            List<Parada> paradas = recorrido.getParadas();
            duracionTotal += calcularDuracionTramo(paradas, tramos);
        }

        return duracionTotal;
    }

    // Método auxiliar para imprimir resultados en el formato solicitado
    public void imprimirResultados(Parada origen, Parada destino, LocalTime horaLlegada,
                                   List<List<Recorrido>> resultados, Map<String, Tramo> tramos) {
        System.out.println("Parada origen: Parada [codigo=" + origen.getCodigo() +
                ", direccion=" + origen.getDireccion() + "]");
        System.out.println("Parada destino: Parada [codigo=" + destino.getCodigo() +
                ", direccion=" + destino.getDireccion() + "]");
        System.out.println("Llega a la parada: " + horaLlegada);

        for (List<Recorrido> opcion : resultados) {
            System.out.println("============================");

            int duracionTotal = 0;
            LocalTime horaLlegadaFinal = horaLlegada;

            for (Recorrido recorrido : opcion) {
                System.out.println("Linea: " + recorrido.getLinea().getNombre());
                System.out.print("Paradas: [");
                List<Parada> paradas = recorrido.getParadas();
                for (int i = 0; i < paradas.size(); i++) {
                    Parada p = paradas.get(i);
                    System.out.print("Parada [codigo=" + p.getCodigo() + ", direccion=" + p.getDireccion() + "]");
                    if (i < paradas.size() - 1) System.out.print(", ");
                }
                System.out.println("]");

                int duracion = calcularDuracionTramo(paradas, tramos);
                duracionTotal += duracion;

                System.out.println("Hora de Salida: " + recorrido.getHoraSalida());
                System.out.println("Duración: " + formatearDuracion(duracion));
                System.out.println("============================");

                horaLlegadaFinal = recorrido.getHoraSalida().plusMinutes(duracion);
            }

            System.out.println("Duración total: " + formatearDuracion(duracionTotal));
            System.out.println("Hora de llegada: " + horaLlegadaFinal);
            System.out.println("============================");
        }
    }

    private String formatearDuracion(int minutos) {
        int horas = minutos / 60;
        int mins = minutos % 60;
        return String.format("%02d:%02d", horas, mins);
    }
}