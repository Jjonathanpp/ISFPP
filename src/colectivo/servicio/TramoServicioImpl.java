package colectivo.servicio;

import colectivo.conexion.Factory;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Tramo;
import java.util.Map;

public class TramoServicioImpl implements TramoServicio {
    private TramoDAO tramoDAO;

    public TramoServicioImpl() {
        tramoDAO = (TramoDAO) Factory.getInstancia("TRAMO");
    }

    @Override
    public void insertar(Tramo tramo) {
        try {
            tramoDAO.insertar(tramo);
        } catch (InstanciaNoExisteException e) {
            throw new RuntimeException(e);
        } catch (InstanciaExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void actualizar(Tramo tramo) {
        tramoDAO.actualizar(tramo);
    }

    @Override
    public void borrar(Tramo tramo) {
        try {
            tramoDAO.borrar(tramo);
        } catch (InstanciaNoExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        return tramoDAO.buscarTodos();
    }



}
