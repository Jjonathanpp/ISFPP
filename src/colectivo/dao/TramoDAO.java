package colectivo.dao;

import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Tramo;

import java.util.Map;

public interface TramoDAO {

    void insertar (Tramo tramo) throws InstanciaNoExisteEnBDException, InstanciaExisteEnBDException;

    void actualizar (Tramo tramo);

    void borrar(Tramo tramo) throws InstanciaNoExisteEnBDException;

    Map<String, Tramo> buscarTodos();
}
