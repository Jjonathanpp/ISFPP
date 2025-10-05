package colectivo.logica;

//Modo de prueba
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Calculo {



    //metodo Calcular el recorrido
    public  List<List<Recorrido>> calcularRecorrido(Parada paradaOrigen, Parada paradaDestino, int diaSemana,
                                                    LocalTime horaLlegadaParada, Map<String, Tramo> tramos) {

      d

        // 1. Consultar los tramos y horarios desde DAO
        // 2. Buscar rutas directas
        // 3. Si no hay, buscar combinaciones de líneas
        // 4. Calcular tiempos y horarios de paso
        // 5. Retornar lista de recorridos posibles


        return null;
    }

}
