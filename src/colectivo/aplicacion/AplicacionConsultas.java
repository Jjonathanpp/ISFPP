package colectivo.aplicacion;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import colectivo.datos.CargarDatos;
import colectivo.datos.CargarParametros;
import colectivo.interfaz.Interfaz;
import colectivo.logica.Calculo;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;

public class AplicacionConsultas {

    public static void main(String[] args) throws IOException {

        try {
            CargarParametros.parametros(); // Carga los parametros de texto
        } catch (IOException e) {
            System.err.print("Error al cargar parametros");
            System.exit(-1);
        }

        Map<Integer, Parada> paradas = CargarDatos.cargarParadas(CargarParametros.getArchivoParada());

        Map<String, Linea> lineas = CargarDatos.cargarLineas(CargarParametros.getArchivoLinea(),
                CargarParametros.getArchivoFrecuencia(), paradas);

        Map<String, Tramo> tramos = CargarDatos.cargarTramos(CargarParametros.getArchivoTramo(), paradas);

        // Ingreso datos usuario

        Parada paradaOrigen = Interfaz.ingresarParadaOrigen(paradas);
        Parada paradaDestino = Interfaz.ingresarParadaDestino(paradas);
        int diaSemana = Interfaz.ingresarDiaSemana();
        LocalTime horaLlegaParada = Interfaz.ingresarHoraLlegaParada();


        // Realizar c�lculo
        List<List<Recorrido>> recorridos = Calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana, horaLlegaParada, tramos);

        // Mostrar resultado
        Interfaz.resultado(recorridos, paradaOrigen, paradaDestino, horaLlegaParada);

    }
}
