package colectivo.dao.secuencial;

import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

import java.io.File;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;

public class TramoSecuencialDAO implements TramoDAO {

    private final String name;
    private final ParadaDAO paradaDAO;  // para resolver id -> Parada

    public TramoSecuencialDAO() {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        this.name = rb.getString("tramo");
        this.paradaDAO = new ParadaSecuencialDAO();
    }

    /* ===================== Helpers de clave ===================== */

    private String keyFromIds(int idIni, int idFin, int tipo) {
        return idIni + ";" + idFin + ";" + tipo;
    }

    private String keyFromParadas(String codIni, String codFin, int tipo) {
        int idIni = Integer.parseInt(codIni);
        int idFin = Integer.parseInt(codFin);
        return keyFromIds(idIni, idFin, tipo);
    }

    /* ============ Lectura / Escritura del archivo ============== */

    // Lee todo: Map<"inicio;fin;tipo", Tramo>
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
                    System.err.println("Tramo ignorado: paradas inexistentes (" + idIni + " → " + idFin + ")");
                }

                if (in.hasNextLine()) in.nextLine();
            }
        } catch (Exception e) {
            System.err.println("Error al leer archivo de tramos: " + e.getMessage());
        }
        return mapa;
    }

    // Escribe todo el Map al archivo (orden: inicio, fin, tipo)
    private void escribirArchivo(Map<String, Tramo> mapa) {
        try (Formatter out = new Formatter(new File("src/resources/" + name), "UTF-8")) {
            //Ordena los Tramos 1.º por IdIni, si son iguales por IdFin y si también son iguales por tipo(1 o 2)
            mapa.values().stream()
                    .sorted(Comparator
                            .comparing((Tramo t) -> Integer.parseInt(t.getInicio().getCodigo()))
                            .thenComparing(t -> Integer.parseInt(t.getFin().getCodigo()))
                            .thenComparingInt(Tramo::getTipo))
                    .forEach(t -> {
                        //escribe el Archivo linea a linea
                        int idIni = Integer.parseInt(t.getInicio().getCodigo());
                        int idFin = Integer.parseInt(t.getFin().getCodigo());
                        out.format("%d;%d;%d;%d;%n", idIni, idFin, t.getTiempo(), t.getTipo());
                    });
        } catch (Exception e) {
            System.err.println("Error al escribir archivo de tramos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ================== Validaciones de existencia ================= */

    // True si la parada EXISTE (por código String)
    private boolean existeParada(String codigoParada) {
        try {
            int id = Integer.parseInt(codigoParada);
            return paradaDAO.buscarTodos().containsKey(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // True si el tramo EXISTE (misma identidad: inicio;fin;tipo)
    private boolean existeTramo(String codIni, String codFin, int tipo) {
        return leerDesdeArchivo().containsKey(keyFromParadas(codIni, codFin, tipo));
    }

    /* ======================== Métodos del DAO ======================= */

    @Override
    public void insertar(Tramo tramo) {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        if (!existeParada(codIni) || !existeParada(codFin)) {
            System.err.println("No se puede insertar: alguna parada no existe (" + codIni + " → " + codFin + ")");
            return;
        }
        if (existeTramo(codIni, codFin, tipo)) {
            System.err.println("Tramo ya existente (" + codIni + " → " + codFin + ", tipo " + tipo + ")");
            return;
        }

        Map<String, Tramo> mapa = leerDesdeArchivo();
        mapa.put(keyFromParadas(codIni, codFin, tipo), tramo);
        escribirArchivo(mapa);
        System.out.println("Tramo insertado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public void actualizar(Tramo tramo) {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        if (!existeParada(codIni) || !existeParada(codFin)) {
            System.err.println("No se puede actualizar: alguna parada no existe (" + codIni + " → " + codFin + ")");
            return;
        }
        if (!existeTramo(codIni, codFin, tipo)) {
            System.err.println("No existe el tramo para actualizar (" + codIni + " → " + codFin + ", tipo " + tipo + ")");
            return;
        }

        Map<String, Tramo> mapa = leerDesdeArchivo();
        mapa.put(keyFromParadas(codIni, codFin, tipo), tramo);
        escribirArchivo(mapa);
        System.out.println("Tramo actualizado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public void borrar(Tramo tramo) throws InstanciaNoExisteEnBDException {
        String codIni = tramo.getInicio().getCodigo();
        String codFin = tramo.getFin().getCodigo();
        int tipo      = tramo.getTipo();

        String k = keyFromParadas(codIni, codFin, tipo);
        Map<String, Tramo> mapa = leerDesdeArchivo();

        if (!mapa.containsKey(k)) {
            throw new InstanciaNoExisteEnBDException(
                    "No existe el tramo: " + codIni + " → " + codFin + " (tipo " + tipo + ")"
            );
        }

        mapa.remove(k);
        escribirArchivo(mapa);
        System.out.println("Tramo borrado: " + codIni + " → " + codFin + " (tipo " + tipo + ")");
    }

    @Override
    public Map<String, Tramo> buscarTodos() {
        return leerDesdeArchivo();
    }
}
