package colectivo.dao;

import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Parada;

import java.util.Map;

public interface ParadaDAO {

    void insertar(Parada parada) throws InstanciaExisteException;

    void actualizar(Parada parada);

    void borrar(Parada parada) throws InstanciaNoExisteException;

    Map<Integer, Parada> buscarTodos();
}
