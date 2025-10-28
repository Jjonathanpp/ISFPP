package colectivo.logica;

import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface RecorridoDesacoplador {
    List<List<Recorrido>> buscarRecorridos(Parada origen, Parada destino, int dia, LocalTime hora, Map<String, ?> tramos);
}

//Esta interface es para que si se cambia TODO de calculo pero se deja el metodo buscar recorridos (que es obligatorio)
//no haya que cambiar nada en el resto del codigo. Solo se crea una nueva clase que implemente esta interface y listo.
//Desacopla perfectamente.
