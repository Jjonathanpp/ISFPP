package colectivo.servicio;

import colectivo.conexion.Factory;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Parada;
import java.util.Map;

public class ParadaServicioImpl implements ParadaServicio{
    private ParadaDAO paradaDAO;

    public ParadaServicioImpl(){
        this.paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
    }

    @Override
    public void insertar(Parada parada) {
        try {
            paradaDAO.insertar(parada);
        } catch (InstanciaExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void actualizar(Parada parada) {
        paradaDAO.actualizar(parada);
    }

    @Override
    public void borrar(Parada parada) {
        try {
            paradaDAO.borrar(parada);
        } catch (InstanciaNoExisteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        return paradaDAO.buscarTodos();
    }


}
