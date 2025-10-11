package colectivo.interfaz;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;

public class Interfaz {

    // Usuario ingresa parada origen
    public static Parada ingresarParadaOrigen(Map<Integer, Parada> paradas) {
        return null;
    }

    // Usuario ingresa parada destino
    public static Parada ingresarParadaDestino(Map<Integer, Parada> paradas) {
        return null;
    }

    // Usuario ingresa d�a de la semana (1=lunes, 2=martes, ... 7=domingo)
    public static int ingresarDiaSemana() {
        return 1;
    }

    // Usuario ingresa hora de llegada a la parada
    public static LocalTime ingresarHoraLlegaParada() {
        return LocalTime.of(10, 35);
    }

    // Mostrar los resultados
    public static void resultado(List<List<Recorrido>> listaRecorridos, Parada paradaOrigen, Parada paradaDestino,
                                 LocalTime horaLlegaParada) {
    }

}
