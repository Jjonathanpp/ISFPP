package colectivo.dao.secuencial;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;

import java.io.File;
import java.util.*;

public class LineaSecuencialDAO implements LineaDAO {

    private final String name;
    private final ParadaDAO paradaDAO;  // para resolver IDs -> Parada

    public LineaSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        this.name = rb.getString("linea");
        this.paradaDAO = new ParadaSecuencialDAO();
    }

    // ===================== Privados reutilizables =====================

    /**
     * Lee todo el archivo de líneas y devuelve Map<codigo, Linea>
     */
    private Map<String, Linea> leerDesdeArchivo() {
        Map<String, Linea> mapa = new HashMap<>();
        Map<Integer, Parada> idxParadas = paradaDAO.buscarTodos();

        try (Scanner in = new Scanner(new File("src/resources/" + name), "UTF-8")) {
            in.useDelimiter("\\s*;\\s*");

            while (in.hasNext()) {
                // codigo y nombre son los dos primeros tokens
                String codigo = in.next();
                if (!in.hasNext()) break;
                String nombre = in.next();

                // leer IDs de paradas hasta que deje de haber enteros (fin de línea)
                List<Parada> paradas = new ArrayList<>();
                while (in.hasNextInt()) {
                    int idParada = in.nextInt();
                    Parada p = idxParadas.get(idParada);
                    if (p != null) {
                        paradas.add(p);
                    } else {
                        System.err.println(" Parada " + idParada + " no encontrada para línea " + codigo);
                    }
                }
                if (in.hasNextLine()) in.nextLine();

                // validar mínimo 2 paradas
                if (paradas.size() >= 2) {
                    Linea l = new Linea(codigo, nombre, paradas);
                    mapa.put(codigo, l);
                } else {
                    System.err.println(" Línea " + codigo + " ignorada: menos de 2 paradas");
                }
            }
        } catch (Exception e) {
            System.err.println(" Error al leer archivo de líneas: " + e.getMessage());
        }

        return mapa;
    }

    /**
     * Escribe todo el contenido al archivo (ordenado por código para estabilidad).
     */
    private void escribirArchivo(Map<String, Linea> mapa) {
        try (Formatter out = new Formatter(new File("src/resources/" + name), "UTF-8")) {
            mapa.values().stream()
                    .sorted(Comparator.comparing(Linea::getCodigo))
                    .forEach(l -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(l.getCodigo()).append(';')
                                .append(l.getNombre()).append(';');
                        for (Parada p : l.getParadas()) {
                            // el archivo guarda IDs numéricos de parada
                            int id = Integer.parseInt(p.getCodigo());
                            sb.append(id).append(';');
                        }
                        out.format("%s%n", sb.toString());
                    });
        } catch (Exception e) {
            System.err.println(" Error al escribir archivo de líneas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean existeLinea(String codigo) {
        return leerDesdeArchivo().containsKey(codigo);
    }

    // ========================= Métodos del DAO ========================

    @Override
    public Map<String, Linea> buscarTodos() {
        return leerDesdeArchivo();
    }

    @Override
    public void insertar(Linea linea) throws InstanciaExisteEnBDException {
        if (existeLinea(linea.getCodigo())) {
            throw new InstanciaExisteEnBDException("Ya existe la línea: " + linea.getCodigo());
        }
        if (linea.getParadas() == null || linea.getParadas().size() < 2) {
            throw new IllegalArgumentException("Una línea debe tener al menos dos paradas");
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.put(linea.getCodigo(), linea);
        escribirArchivo(mapa);
        System.out.println(" Línea insertada: " + linea.getCodigo());
    }

    @Override
    public void actualizar(Linea linea) {
        if (!existeLinea(linea.getCodigo())) {
            System.err.println(" No existe la línea " + linea.getCodigo() + " para actualizar.");
            return;
        }
        if (linea.getParadas() == null || linea.getParadas().size() < 2) {
            System.err.println(" La línea " + linea.getCodigo() + " debe tener al menos dos paradas.");
            return;
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.put(linea.getCodigo(), linea);
        escribirArchivo(mapa);
        System.out.println(" Línea actualizada: " + linea.getCodigo());
    }

    @Override
    public void borrar(Linea linea) throws InstanciaNoExisteEnBDException {
        if (!existeLinea(linea.getCodigo())) {
            throw new InstanciaNoExisteEnBDException("No existe la línea: " + linea.getCodigo());
        }
        Map<String, Linea> mapa = leerDesdeArchivo();
        mapa.remove(linea.getCodigo());
        escribirArchivo(mapa);
        System.out.println(" Línea borrada: " + linea.getCodigo());
    }
}
