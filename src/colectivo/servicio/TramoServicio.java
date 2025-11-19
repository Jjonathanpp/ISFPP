package colectivo.servicio;

import colectivo.modelo.Tramo;
import java.util.Map;

public interface TramoServicio {

    void insertar(Tramo tramo);

    void actualizar(Tramo tramo);

    void borrar(Tramo tramo);

    Map<String, Tramo> buscarTodos();
}
