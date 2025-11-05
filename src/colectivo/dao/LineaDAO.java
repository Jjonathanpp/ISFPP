package colectivo.dao;

import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Linea;

import java.util.Map;

public interface LineaDAO {


    void insertar(Linea linea) throws InstanciaExisteException;

    void actualizar(Linea linea);

    void borrar(Linea linea) throws InstanciaNoExisteException;

    Map<String, Linea> buscarTodos();
}
