package colectivo.dao;

import colectivo.modelo.Linea;

import java.util.Map;

public interface LineaDAO {


    void insertar(Linea linea);

    void actualizar(Linea linea);

    void borrar(Linea linea);

    Map<String, Linea> buscarTodos();
}
