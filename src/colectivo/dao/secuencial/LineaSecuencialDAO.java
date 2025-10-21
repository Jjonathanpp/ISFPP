package colectivo.dao.secuencial;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Frecuencia;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalTime;
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
     * Lee todo el archivo de líneas y devuelve Map<codigo, Linea> (sin frecuencias).
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
        // 1) Leemos las líneas y sus paradas (sin frecuencias)
        Map<String, Linea> mapa = leerDesdeArchivo();

        // 2) Leemos mapa de frecuencias desde archivo (clave: codigoLinea -> lista de (dia,hora))
        Map<String, List<FrecuenciaData>> frecuenciasMapa = leerFrecuenciasDesdeArchivo();

        // 3) Asociamos las frecuencias a las instancias reales de Linea
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

    // ===================== Frecuencias desde archivo =====================

    // Estructura interna para datos temporales de frecuencia
    private record FrecuenciaData(int diaSemana, LocalTime hora) {}

    /**
     * Lee un archivo de frecuencias y devuelve un mapa: codigoLinea -> lista de FrecuenciaData.
     * Formato por línea (separador ';'):
     *   COD_LINEA;DIA_SEMANA;HH:mm
     * Ejemplo:
     *   L3I;1;10:00
     *   L3I;1;11:30
     *
     * El nombre del archivo se obtiene de config.properties (clave "frecuencia").
     */
    private Map<String, List<FrecuenciaData>> leerFrecuenciasDesdeArchivo() {
        Map<String, List<FrecuenciaData>> mapa = new HashMap<>();
        try {
            ResourceBundle rb = ResourceBundle.getBundle("config");
            String nombreArchivo = rb.getString("frecuencia").trim(); // p.e. "frecuencia.txt"
            File f = new File("src/resources/" + nombreArchivo);
            if (!f.exists()) {
                // archivo opcional: si no existe devolvemos mapa vacío
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
            System.err.println("Error leyendo frecuencias: " + e.getMessage());
        }
        return mapa;
    }
}
