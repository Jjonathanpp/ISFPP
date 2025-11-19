package colectivo.servicio;

import colectivo.modelo.Parada;
import java.util.Map;

public interface ParadaServicio {
    void insertar(Parada parada);

    void actualizar(Parada parada);

    void borrar(Parada parada);

    Map<Integer, Parada> buscarTodos();
}
