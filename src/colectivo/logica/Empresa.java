package colectivo.logica;

import colectivo.aplicacion.Coordinador;
import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.apache.log4j.Logger;


import java.util.List;
import java.util.Map;

public class Empresa {
    private static final Logger LOGGER = Logger.getLogger(Empresa.class);

    //Empresa
    private static Empresa empresa = null;
    //DAO mapas
    private Map<Integer, Parada> paradas;
    private Map<String, Linea> lineas;
    private Map<String, Tramo> tramos;
    //DAO instancias
    private final ParadaDAO paradaDAO;
    private final LineaDAO lineaDAO;
    private final TramoDAO tramoDAO;

    public static Empresa getEmpresa(){
        if(empresa == null){
            empresa = new Empresa();
        }
        return empresa;
    }

    /**
     * Se cambio la forma en la que se cargan los mapas desde los DAO.
     */
    private Empresa(){
        super();
        this.paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
        this.lineaDAO  = (LineaDAO)  Factory.getInstancia("LINEA");
        this.tramoDAO  = (TramoDAO)  Factory.getInstancia("TRAMO");

        paradas = paradaDAO.buscarTodos();
        lineas  = lineaDAO.buscarTodos();
        tramos  = tramoDAO.buscarTodos();
    }

    //Lineas
    public void agregarLinea(Linea linea){
        if(verificacionExistenciaLinea(linea)) {
            return;
        }
        try {
            lineaDAO.insertar(linea);
            lineas.put(linea.getCodigo(), linea);
            LOGGER.info("Línea agregada correctamente: " + linea.getCodigo());
        } catch (InstanciaExisteEnBDException e) {
            LOGGER.error("Error al insertar línea en persistencia: " + linea.getCodigo(), e);
        }
    }
    public void modificarLinea(Linea linea){
        if(verificacionExistenciaLinea(linea)) {
            return;
        }
        try {
            lineaDAO.actualizar(linea);
            lineas.replace(linea.getCodigo(), linea);
            LOGGER.info("Línea modificada correctamente: " + linea.getCodigo());
        } catch (Exception e) {
            LOGGER.error("Error al modificar línea en persistencia: " + linea.getCodigo(), e);
        }
    }
    public void eliminarLinea(Linea linea) throws Exception {
        if(verificacionExistenciaLinea(linea)) {
            return;
        }
        try {
            lineaDAO.borrar(linea);
            lineas.remove(linea.getCodigo());
            LOGGER.info("Línea eliminada correctamente: " + linea.getCodigo());
        } catch (Exception e) {
            LOGGER.error("Error al eliminar línea en persistencia: " + linea.getCodigo(), e);
            throw e;
        }
    }

    public Linea buscarLinea(Linea linea){
        if (lineas.get(linea) != null) {
            return lineas.get(linea.getCodigo());
        }
        LOGGER.warn("La línea a buscar es nula.");
        return null;
    }

    private boolean verificacionExistenciaLinea(Linea linea) {
        if(linea == null) {
            LOGGER.warn("La línea a agregar es nula.");
            return true;
        }
        else if(lineas.get(linea.getCodigo()) != null) {
            LOGGER.warn("La línea ya existe: " + linea.getCodigo());
            return true;
        }
        else return false;
    }

    //Paradas
    public void agregarParada(Parada parada){
        if(verificacionExistenciaParada(parada)) {
            return;
        }
        int id = parseIntParada(parada);
        if(id == -1) { return; }
        try {
            // Si el ID se genera en la BD, el DAO debería devolver el id o la entidad con id asignado.
            paradaDAO.insertar(parada);
            paradas.put(id, parada);
            LOGGER.info("Parada agregada correctamente: " + parada.getCodigo());
        } catch (Exception e) {
            LOGGER.error("Error al insertar parada en persistencia: " + parada.getCodigo(), e);
        }
    }
    public void modificarParada(Parada parada){
        if(verificacionExistenciaParada(parada)) {
            return;
        }
        int id = parseIntParada(parada);
        if(id == -1) { return; }
        try {
            paradaDAO.actualizar(parada);
            paradas.replace(id, parada);
            LOGGER.info("Parada modificada correctamente: " + parada.getCodigo());
        } catch (Exception e) {
            LOGGER.error("Error al modificar parada en persistencia: " + parada.getCodigo(), e);
        }
    }
    public void eliminarParada(Parada parada) throws Exception {
        if(verificacionExistenciaParada(parada)) {
            return;
        }
        int id = parseIntParada(parada);
        if(id == -1) { return; }
        try {
            paradaDAO.borrar(parada);
            paradas.remove(id);
            LOGGER.info("Parada eliminada correctamente: " + parada.getCodigo());
        } catch (Exception e) {
            LOGGER.error("Error al eliminar parada en persistencia: " + parada.getCodigo(), e);
            throw e;
        }
    }

    private boolean verificacionExistenciaParada(Parada parada) {
        if(parada == null) {
            LOGGER.warn("La parada a agregar es nula.");
            return true;
        }
        else if(lineas.get(parada.getCodigo()) != null) {
            LOGGER.warn("La línea ya existe: " + parada.getCodigo());
            return true;
        }
        else return false;
    }
    private int parseIntParada(Parada parada){
        int id;
        try {
            id = Integer.parseInt(parada.getCodigo());
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Código de parada inválido: " + parada.getCodigo(), nfe);
            return -1;
        }
        return id;
    }

    //Tramos
    public void agregarTramo(Tramo tramo){
        String key = verificacionExistenciaTramo(tramo);
        if(key.equals("-1")) { return; }
        try {
            tramoDAO.insertar(tramo);
            tramos.put(key, tramo);
            LOGGER.info("Tramo agregado correctamente: " + key);
        } catch (Exception e) {
            LOGGER.error("Error al insertar tramo en persistencia: " + key, e);
        }
    }
    public void modificarTramo(Tramo tramo){
        String key = verificacionExistenciaTramo(tramo);
        if(key.equals("-1")) { return; }
        try {
            tramoDAO.actualizar(tramo);
            tramos.replace(key, tramo);
            LOGGER.info("Tramo modificado correctamente: " + key);
        } catch (Exception e) {
            LOGGER.error("Error al modificar tramo en persistencia: " + key, e);
        }
    }
    public void eliminarTramo(Tramo tramo) throws Exception {
        String key = verificacionExistenciaTramo(tramo);
        if(key.equals("-1")) { return; }
        try {
            tramoDAO.borrar(tramo);
            tramos.remove(key);
            LOGGER.info("Tramo eliminado correctamente: " + key);
        } catch (Exception e) {
            LOGGER.error("Error al eliminar tramo en persistencia: " + key, e);
            throw e;
        }
    }

    private String verificacionExistenciaTramo(Tramo tramo) {
        if(tramo == null) {
            LOGGER.warn("El tramo a agregar es nula.");
            return "-1";
        }
        String key = tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo();
        if(lineas.get(key) != null) {
            LOGGER.warn("El tramo ya existe: " + key);
            return "-1";
        }
        return key;
    }
    //-----------------------------

    //Recuperar los datos de DAO

    public Map<String, Tramo> getTramos() {
        return tramos;
    }
    public Map<Integer, Parada> getParadas() {
        return paradas;
    }
    public Map<String, Linea> getLineas() {
        return lineas;
    }


}
