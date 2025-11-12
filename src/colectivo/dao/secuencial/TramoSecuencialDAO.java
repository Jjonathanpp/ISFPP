package colectivo.dao.secuencial;

import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;

public class TramoSecuencialDAO implements TramoDAO {

    private static final Logger LOGGER = Logger.getLogger(TramoSecuencialDAO.class);

    private final String name;
    private final ParadaDAO paradaDAO;

    public TramoSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        this.name = rb.getString("tramo");
        this.paradaDAO = new ParadaSecuencialDAO();
    }

    private String keyFromIds(int idIni, int idFin, int tipo) {
        return idIni + ";" + idFin + ";" + tipo;
    }

    private String keyFromParadas(String codIni, String codFin, int tipo) {
        int idIni = Integer.parseInt(codIni);
        int idFin = Integer.parseInt(codFin);
        return keyFromIds(idIni, idFin, tipo);
    }

    private Map<String, Tramo> leerDesdeArchivo() {
        Map<String, Tramo> mapa = new HashMap<>();
        Map<Integer, Parada> idxParadas = paradaDAO.buscarTodos();

        try (Scanner in = new Scanner(new File("src/resources/" + name), "UTF-8")) {
            in.useDelimiter("\\s*;\\s*");

            while (in.hasNextInt()) {
                int idIni  = in.nextInt();
                int idFin  = in.nextInt();
                int tiempo = in.nextInt();
                int tipo   = in.nextInt();

                Parada pIni = idxParadas.get(idIni);
                Parada pFin = idxParadas.get(idFin);

                if (pIni != null && pFin != null) {
                    Tramo t = new Tramo(tiempo, tipo, pIni, pFin);
                    mapa.put(keyFromIds(idIni, idFin, tipo), t);
                } else {
                    LOGGER.warn("Tramo ignorado: paradas inexistentes (" + idIni + " → " + idFin + ")");
                }

                if (in.hasNextLine()) in.nextLine();
            }
        } catch (Exception e) {
            LOGGER.error("Error al leer archivo de tramos", e);
        }
        return mapa;
    }

    private void escribirArchivo(Map<String, Tramo> mapa) {
        try (Formatter out = new Formatter(new File("src/resources/" + name), "UTF-8")) {
            mapa.values().stream()
                    .sorted(Comparator
                            .comparing((Tramo t) -> Integer.parseInt(t.getInicio().getCodigo()))
                            .thenComparing(t -> Integer.parseInt(t.getFin().getCodigo()))
                            .thenComparingInt(Tramo::getTipo))
                    .forEach(t -> {
                        int idIni = Integer.parseInt(t.getInicio().getCodigo());
                        int idFin = Integer.parseInt(t.getFin().getCodigo());
                        out.format("%d;%d;%d;%d;%n", idIni, idFin, t.getTiempo(), t.getTipo());
                    });
        } catch (Exception e) {
            LOGGER.error("Error al escribir archivo de tramos", e);
        }
    }

    private boolean existeParada(String codigoParada) {
        try {
            int id = Integer.parseInt(codigoParada);
            return paradaDAO.buscarTodos().containsKey(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean existeTramo(String codIni, String codFin, int tipo) {
        return leerDesdeArchivo().containsKey(keyFromParadas(codIni, codFin, tipo));
    }

    @Override
    public void insertar(Tramo tramo) {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        if (!existeParada(codIni) || !existeParada(codFin)) {
            LOGGER.warn("No se puede insertar: alguna parada no existe (" + codIni + " → " + codFin + ")");
            return;
        }
        if (existeTramo(codIni, codFin, tipo)) {
            LOGGER.warn("Tramo ya existente (" + codIni + " → " + codFin + ", tipo " + tipo + ")");
            return;
        }

        Map<String, Tramo> mapa = leerDesdeArchivo();
        mapa.put(keyFromParadas(codIni, codFin, tipo), tramo);
        escribirArchivo(mapa);
        LOGGER.info("Tramo insertado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public void actualizar(Tramo tramo) {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        if (!existeParada(codIni) || !existeParada(codFin)) {
            LOGGER.warn("No se puede actualizar: alguna parada no existe (" + codIni + " → " + codFin + ")");
            return;
        }
        if (!existeTramo(codIni, codFin, tipo)) {
            LOGGER.warn("No existe el tramo para actualizar (" + codIni + " → " + codFin + ", tipo " + tipo + ")");
            return;
        }

        Map<String, Tramo> mapa = leerDesdeArchivo();
        mapa.put(keyFromParadas(codIni, codFin, tipo), tramo);
        escribirArchivo(mapa);
        LOGGER.info("Tramo actualizado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public void borrar(Tramo tramo) throws InstanciaNoExisteException {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        String k = keyFromParadas(codIni, codFin, tipo);
        Map<String, Tramo> mapa = leerDesdeArchivo();

        if (!mapa.containsKey(k)) {
            throw new InstanciaNoExisteException(
                    "No existe el tramo: " + codIni + " → " + codFin + " (tipo " + tipo + ")"
            );
        }

        mapa.remove(k);
        escribirArchivo(mapa);
        LOGGER.info("Tramo borrado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        return leerDesdeArchivo();
    }
}
