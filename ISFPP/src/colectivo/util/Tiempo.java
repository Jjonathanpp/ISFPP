package colectivo.util;


import java.time.LocalTime;

public class Tiempo {

    public static LocalTime segundosATiempo(int totalSegundos) {

        // Calcular horas
        int horas = totalSegundos / 3600;
        int segundosRestantes = totalSegundos % 3600;

        // Calcular minutos
        int minutos = segundosRestantes / 60;
        int segundos = segundosRestantes % 60;

        return LocalTime.of(horas, minutos, segundos);
    }
}
