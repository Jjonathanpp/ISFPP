package colectivo.dao;

import colectivo.modelo.Tramo;

import java.util.Map;

public interface TramoDAO {

    void insertar (Tramo tramo);

    void actualizar (Tramo tramo);

    void borrar(Tramo tramo);

    Map<String, Tramo> buscarTodos();
}
