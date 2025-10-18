package colectivo.dao;

import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;

import java.util.Map;

public interface ParadaDAO {

    void insertar(Parada parada) throws InstanciaExisteEnBDException;

    void actualizar(Parada parada);

    void borrar(Parada parada) throws InstanciaNoExisteEnBDException;

    Map<Integer, Parada> buscarTodos();
}
