package colectivo.dao;

import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Tramo;

import java.util.Map;

public interface TramoDAO {

    void insertar (Tramo tramo) throws InstanciaNoExisteException, InstanciaExisteException;

    void actualizar (Tramo tramo);

    void borrar(Tramo tramo) throws InstanciaNoExisteException;

    Map<String, Tramo> buscarTodos();
}
