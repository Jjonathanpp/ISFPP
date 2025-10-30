package colectivo.dao.secuencial;

import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.*;

public class ParadaSecuencialDAO implements ParadaDAO {

    private static final Logger LOGGER = Logger.getLogger(ParadaSecuencialDAO.class);

    private final String name;

    public ParadaSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        name = rb.getString("parada"); // "parada_PM.txt"
    }

    private Map<Integer, Parada> leerDesdeArchivo() {
        Map<Integer, Parada> mapa = new HashMap<>();

        try (Scanner inFile = new Scanner(new File("src/resources/" + name), "UTF-8")) {
            inFile.useDelimiter("\\s*;\\s*");

            while (inFile.hasNextInt()) {
                String codigo = String.valueOf(inFile.nextInt());
                String direccion = inFile.next();
                String latitudToken = inFile.next();
                String longitudToken = inFile.next();

                double latitud;
                double longitud;
                try {
                    latitud = parseDecimal(latitudToken);
                    longitud = parseDecimal(longitudToken);
                } catch (NumberFormatException ex) {
                    LOGGER.warn("Error al parsear coordenadas de la parada " + codigo + ": " + ex.getMessage());
                    if (inFile.hasNextLine()) inFile.nextLine();
                    continue;
                }

                Parada parada = new Parada(codigo, direccion, latitud, longitud);
                mapa.put(Integer.parseInt(codigo), parada);

                if (inFile.hasNextLine()) inFile.nextLine();
            }

        } catch (Exception e) {
            LOGGER.error("Error al leer archivo de paradas", e);
        }

        return mapa;
    }

    private double parseDecimal(String token) {
        String normalizado = token.trim().replace(',', '.');
        return Double.parseDouble(normalizado);
    }

    private void escribirArchivo(Map<Integer, Parada> mapa) {
        try (Formatter outFile = new Formatter(new File("src/resources/" + name), "UTF-8")) {
            mapa.entrySet().stream().sorted(Map.Entry.comparingByKey()) // ordena por ID
                    .forEach(entry -> {
                        Parada p = entry.getValue();
                        outFile.format("%s;%s;%.6f;%.6f;%n",
                                p.getCodigo(), p.getDireccion(), p.getLatitud(), p.getLongitud());
                    });
        } catch (Exception e) {
            LOGGER.error("Error al escribir archivo de paradas", e);
        }
    }

    @Override
    public void insertar(Parada parada) throws InstanciaExisteEnBDException {
        if (existe(parada.getCodigo())) {
            throw new InstanciaExisteEnBDException("Ya existe la parada con código: " + parada.getCodigo());
        }

        Map<Integer, Parada> mapa = leerDesdeArchivo();
        int id = Integer.parseInt(parada.getCodigo());

        mapa.put(id, parada);
        escribirArchivo(mapa);
        LOGGER.info("Parada insertada correctamente: " + parada.getCodigo());
    }


    @Override
    public void actualizar(Parada parada) {
        if (!existe(parada.getCodigo())) {
            LOGGER.warn("No existe la parada con código " + parada.getCodigo() + " para actualizar");
            return;
        }

        Map<Integer, Parada> mapa = leerDesdeArchivo();
        int id = Integer.parseInt(parada.getCodigo());

        mapa.put(id, parada);
        escribirArchivo(mapa);
        LOGGER.info("Parada actualizada correctamente: " + parada.getCodigo());
    }

    @Override
    public void borrar(Parada parada) throws InstanciaNoExisteEnBDException {
        if (!existe(parada.getCodigo())) {
            throw new InstanciaNoExisteEnBDException("No existe la parada con código: " + parada.getCodigo());
        }

        Map<Integer, Parada> mapa = leerDesdeArchivo();
        int id = Integer.parseInt(parada.getCodigo());

        mapa.remove(id);
        escribirArchivo(mapa);
        LOGGER.info("Parada borrada correctamente: " + parada.getCodigo());
    }

    @Override
    public Map<Integer, Parada> buscarTodos() {
        return leerDesdeArchivo();
    }

    // Verifica si ya existe la parada con ese código antes de insertar
    private boolean existe(String codigo) {
        Map<Integer, Parada> mapa = leerDesdeArchivo();
        try {
            int id = Integer.parseInt(codigo);
            return mapa.containsKey(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
