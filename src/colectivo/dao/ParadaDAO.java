package colectivo.dao;

import colectivo.modelo.Parada;

import java.util.Map;

public interface ParadaDAO {

    void insertar(Parada parada);

    void actualizar(Parada parada);

    void borrar(Parada parada);

    Map<Integer, Parada> buscarTodos();
}
