package colectivo.servicio;

import colectivo.modelo.Linea;

import java.util.Map;

public interface LineaServicio {

    void insertar(Linea linea);

    void actualizar(Linea linea);

    void borrar(Linea linea);

    Map<String, Linea> buscarTodos();
}
