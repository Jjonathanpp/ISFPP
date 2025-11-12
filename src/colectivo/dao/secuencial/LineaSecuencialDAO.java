package colectivo.dao.secuencial;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteException;
import colectivo.excepciones.InstanciaNoExisteException;
import colectivo.modelo.Frecuencia;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalTime;
import java.util.*;

public class LineaSecuencialDAO implements LineaDAO {

    private static final Logger LOGGER = Logger.getLogger(LineaSecuencialDAO.class);

    private final String name;
    private final ParadaDAO paradaDAO;

    public LineaSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        this.name = rb.getString("linea");
        this.paradaDAO = new ParadaSecuencialDAO();
    }

    private Map<String, Linea> leerDesdeArchivo() {
        Map<String, Linea> mapa = new HashMap<>();
        Map<Integer, Parada> idxParadas = paradaDAO.buscarTodos();

        try (Scanner in = new Scanner(new File("src/resources/" + name), "UTF-8")) {
            in.useDelimiter("\\s*;\\s*");

            while (in.hasNext()) {
                String codigo = in.next();
                if (!in.hasNext()) break;
                String nombre = in.next();

                List<Parada> paradas = new ArrayList<>();
                while (in.hasNextInt()) {
                    int idParada = in.nextInt();
                    Parada p = idxParadas.get(idParada);
                    if (p != null) {
                        paradas.add(p);
                    } else {
                        LOGGER.warn("Parada " + idParada + " no encontrada para línea " + codigo);
                    }
                }
                if (in.hasNextLine()) in.nextLine();
                if (paradas.size() >= 2) {
                    Linea l = new Linea(codigo, nombre, paradas);
                    mapa.put(codigo, l);
                } else {
                    LOGGER.warn("Línea " + codigo + " ignorada: menos de 2 paradas");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error al leer archivo de líneas", e);
        }

        return mapa;
    }

    private void escribirArchivo(Map<String, Linea> mapa) {
        try (Formatter out = new Formatter(new File("src/resources/" + name), "UTF-8")) {
            mapa.values().stream()
                    .sorted(Comparator.comparing(Linea::getCodigo))
                    .forEach(l -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(l.getCodigo()).append(';')
                                .append(l.getNombre()).append(';');
                        for (Parada p : l.getParadas()) {
                            int id = Integer.parseInt(p.getCodigo());
                            sb.append(id).append(';');
                        }
                        out.format("%s%n", sb.toString());
                    });
        } catch (Exception e) {
            LOGGER.error("Error al escribir archivo de líneas", e);
        }
    }

    private boolean existeLinea(String codigo) {
        return leerDesdeArchivo().containsKey(codigo);
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        Map<String, Linea> mapa = leerDesdeArchivo();
        Map<String, List<FrecuenciaData>> frecuenciasMapa = leerFrecuenciasDesdeArchivo();
        for (Map.Entry<String, Linea> e : mapa.entrySet()) {
            String codigoLinea = e.getKey();
            Linea linea = e.getValue();
            List<FrecuenciaData> datos = frecuenciasMapa.get(codigoLinea);
            if (datos != null) {
                for (FrecuenciaData fd : datos) {
                    linea.agregarFrecuencia(new Frecuencia(linea, fd.diaSemana(), fd.hora()));
                }
            }
        }
        return mapa;
    }

    @Override
    public void insertar(Linea linea) throws InstanciaExisteException {
        if (existeLinea(linea.getCodigo())) {
            throw new InstanciaExisteException("Ya existe la línea: " + linea.getCodigo());
        }
        if (linea.getParadas() == null || linea.getParadas().size() < 2) {
            throw new IllegalArgumentException("Una línea debe tener al menos dos paradas");
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.put(linea.getCodigo(), linea);
        escribirArchivo(mapa);
        LOGGER.info("Línea insertada: " + linea.getCodigo());
    }

    @Override
    public void actualizar(Linea linea) {
        if (!existeLinea(linea.getCodigo())) {
            LOGGER.warn("No existe la línea " + linea.getCodigo() + " para actualizar");
            return;
        }
        if (linea.getParadas() == null || linea.getParadas().size() < 2) {
            LOGGER.warn("La línea " + linea.getCodigo() + " debe tener al menos dos paradas");
            return;
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.put(linea.getCodigo(), linea);
        escribirArchivo(mapa);
        LOGGER.info("Línea actualizada: " + linea.getCodigo());
    }

    @Override
    public void borrar(Linea linea) throws InstanciaNoExisteException {
        if (!existeLinea(linea.getCodigo())) {
            throw new InstanciaNoExisteException("No existe la línea: " + linea.getCodigo());
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.remove(linea.getCodigo());
        escribirArchivo(mapa);
        LOGGER.info("Línea borrada: " + linea.getCodigo());
    }

    private record FrecuenciaData(int diaSemana, LocalTime hora) {}

    private Map<String, List<FrecuenciaData>> leerFrecuenciasDesdeArchivo() {
        Map<String, List<FrecuenciaData>> mapa = new HashMap<>();
        try {
            ResourceBundle rb = ResourceBundle.getBundle("config");
            String nombreArchivo = rb.getString("frecuencia").trim();
            File f = new File("src/resources/" + nombreArchivo);
            if (!f.exists()) {
                return mapa;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) continue;
                    String[] parts = linea.split("\\s*;\\s*");
                    if (parts.length < 3) continue;
                    String codLinea = parts[0].trim();
                    int dia;
                    try {
                        dia = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    String horaStr = parts[2].trim();
                    LocalTime hora;
                    try {
                        hora = LocalTime.parse(horaStr);
                    } catch (Exception ex) {
                        continue;
                    }
                    mapa.computeIfAbsent(codLinea, k -> new ArrayList<>()).add(new FrecuenciaData(dia, hora));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error leyendo frecuencias", e);
        }
        return mapa;
    }
}
