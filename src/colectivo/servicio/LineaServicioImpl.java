package colectivo.servicio;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import java.util.Map;

public class LineaServicioImpl implements LineaServicio{
    private LineaDAO lineaDAO;

    public LineaServicioImpl(){
        this.lineaDAO = (LineaDAO) Factory.getInstancia("LINEA");
    }

    @Override
    public void insertar(Linea linea) {
        try {
            lineaDAO.insertar(linea);
        } catch (InstanciaExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void actualizar(Linea linea) {
        lineaDAO.actualizar(linea);
    }

    @Override
    public void borrar(Linea linea) {
        try {
            lineaDAO.borrar(linea);
        } catch (InstanciaNoExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        return lineaDAO.buscarTodos();
    }


}
