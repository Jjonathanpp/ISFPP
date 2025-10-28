package colectivo.logica;

import java.util.Map;

public class RecorridoDesacopladorImpl implements RecorridoDesacoplador {
    @Override
    public java.util.List<java.util.List<colectivo.modelo.Recorrido>> buscarRecorridos(colectivo.modelo.Parada origen, colectivo.modelo.Parada destino, int dia, java.time.LocalTime hora, java.util.Map<String, ?> tramos) {
        return Calculo.calcularRecorrido(origen, destino, dia, hora, (Map) tramos);
    }

}
