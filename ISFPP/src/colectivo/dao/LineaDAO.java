package colectivo.dao;

import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Linea;

import java.util.Map;

public interface LineaDAO {


    void insertar(Linea linea) throws InstanciaExisteEnBDException;

    void actualizar(Linea linea);

    void borrar(Linea linea) throws InstanciaNoExisteEnBDException;

    Map<String, Linea> buscarTodos();
}
