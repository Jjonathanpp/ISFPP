package colectivo.test;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.excepciones.InstanciaExisteEnBDException;
import colectivo.excepciones.InstanciaNoExisteEnBDException;
import colectivo.logica.Empresa;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Tests unitarios JUnit5 para las operaciones CRUD de Empresa usando DAOs en memoria (fakes).
 * - No toca la base de datos real ni los archivos.
 * - Reemplaza por reflexión las referencias a DAO y los mapas internos de Empresa.
 */
public class EmpresaDaoTest {
    // Tras cada test, resetear el singleton para evitar side-effects
    @AfterEach
    public void resetSingleton() throws Exception {
        Field f = Empresa.class.getDeclaredField("empresa");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    public void testParadaInsertUpdateDelete() throws Exception {
        Empresa empresa = crearEmpresaConDaosEnMemoria();

        Parada p = new Parada("100", "Calle Falsa 123", -34.0, -58.0);

        // Insertar
        empresa.agregarParada(p);
        Map<Integer, Parada> paradas = empresa.getParadas();
        assertTrue(paradas.containsKey(100), "La parada debe haberse insertado en el mapa");
        assertEquals("Calle Falsa 123", paradas.get(100).getDireccion());

        // Modificar
        p.setDireccion("Calle Verdadera 321");
        empresa.modificarParada(p);
        assertEquals("Calle Verdadera 321", paradas.get(100).getDireccion(), "La dirección debe quedar actualizada");

        // Eliminar
        empresa.eliminarParada(p);
        assertFalse(paradas.containsKey(100), "La parada debe haber sido eliminada del mapa");
    }

    @Test
    public void testTramoInsertUpdateDelete() throws Exception {
        Empresa empresa = crearEmpresaConDaosEnMemoria();

        // Crear dos paradas en la cache para referenciarlas desde el tramo
        Parada p1 = new Parada("1", "Inicio", -34.0, -58.0);
        Parada p2 = new Parada("2", "Fin", -34.1, -58.1);
        empresa.agregarParada(p1);
        empresa.agregarParada(p2);

        Tramo t = new Tramo(300, 1, p1, p2); // tiempo 300s, tipo 1

        // Insertar
        empresa.agregarTramo(t);
        Map<String, Tramo> tramos = empresa.getTramos();
        String key = p1.getCodigo() + "-" + p2.getCodigo();
        assertTrue(tramos.containsKey(key), "El tramo debe haberse insertado en el mapa");

        // Modificar (cambiar tiempo)
        t.setTiempo(360);
        empresa.modificarTramo(t);
        assertEquals(360, tramos.get(key).getTiempo(), "El tiempo del tramo debe haber quedado actualizado");

        // Eliminar
        empresa.eliminarTramo(t);
        assertFalse(tramos.containsKey(key), "El tramo debe haber sido eliminado del mapa");
    }

    @Test
    public void testLineaInsertUpdateDelete() throws Exception {
        Empresa empresa = crearEmpresaConDaosEnMemoria();

        // Crear paradas que pertenecerán a la línea
        Parada p1 = new Parada("10", "Parada A", -34.0, -58.0);
        Parada p2 = new Parada("20", "Parada B", -34.1, -58.1);
        empresa.agregarParada(p1);
        empresa.agregarParada(p2);

        List<Parada> paroList = List.of(p1, p2);
        Linea linea = new Linea("L1", "Linea 1", paroList);

        // Insertar
        empresa.agregarLinea(linea);
        Map<String, Linea> lineas = empresa.getLineas();
        assertTrue(lineas.containsKey("L1"), "La línea debe haberse insertado en el mapa");
        assertEquals("Linea 1", lineas.get("L1").getNombre());

        // Modificar (nombre)
        linea.setNombre("Linea Uno");
        empresa.modificarLinea(linea);
        assertEquals("Linea Uno", lineas.get("L1").getNombre(), "El nombre de la línea debe actualizarse");

        // Eliminar
        empresa.eliminarLinea(linea);
        assertFalse(lineas.containsKey("L1"), "La línea debe haber sido eliminada del mapa");
    }

    // --- Helpers ---

    /**
     * Crea (o resetea) el singleton Empresa y reemplaza sus DAOs y mapas internos por implementaciones en memoria.
     * Retorna la instancia de Empresa lista para pruebas.
     */
    private Empresa crearEmpresaConDaosEnMemoria() throws Exception {
        // Resetear singleton
        Field f = Empresa.class.getDeclaredField("empresa");
        f.setAccessible(true);
        f.set(null, null);

        Empresa empresa = Empresa.getEmpresa(); // crea la instancia real (constructor puede cargar DAOs, lo sobreescribimos)

        // Crear mapas vacíos en memoria
        Map<Integer, Parada> paradasMap = new HashMap<>();
        Map<String, Linea> lineasMap = new HashMap<>();
        Map<String, Tramo> tramosMap = new HashMap<>();

        // Crear DAOs en memoria (fakes)
        ParadaDAO paradaDAO = new ParadaDAO() {
            @Override
            public void insertar(Parada parada) throws InstanciaExisteEnBDException {
                int id = Integer.parseInt(parada.getCodigo());
                if (paradasMap.containsKey(id)) throw new InstanciaExisteEnBDException("Existe");
                paradasMap.put(id, parada);
            }

            @Override
            public void actualizar(Parada parada) {
                int id = Integer.parseInt(parada.getCodigo());
                paradasMap.put(id, parada);
            }

            @Override
            public void borrar(Parada parada) throws InstanciaNoExisteEnBDException {
                int id = Integer.parseInt(parada.getCodigo());
                if (!paradasMap.containsKey(id)) throw new InstanciaNoExisteEnBDException("No existe");
                paradasMap.remove(id);
            }

            @Override
            public Map<Integer, Parada> buscarTodos() {
                return new HashMap<>(paradasMap);
            }
        };

        LineaDAO lineaDAO = new LineaDAO() {
            @Override
            public void insertar(Linea linea) throws InstanciaExisteEnBDException {
                if (lineasMap.containsKey(linea.getCodigo())) throw new InstanciaExisteEnBDException("Existe linea");
                lineasMap.put(linea.getCodigo(), linea);
            }

            @Override
            public void actualizar(Linea linea) {
                lineasMap.put(linea.getCodigo(), linea);
            }

            @Override
            public void borrar(Linea linea) throws InstanciaNoExisteEnBDException {
                if (!lineasMap.containsKey(linea.getCodigo())) throw new InstanciaNoExisteEnBDException("No existe linea");
                lineasMap.remove(linea.getCodigo());
            }

            @Override
            public Map<String, Linea> buscarTodos() {
                return new HashMap<>(lineasMap);
            }
        };

        TramoDAO tramoDAO = new TramoDAO() {
            @Override
            public void insertar(Tramo tramo) throws InstanciaNoExisteEnBDException, InstanciaExisteEnBDException {
                String key = tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo();
                if (tramosMap.containsKey(key)) throw new InstanciaExisteEnBDException("Existe tramo");
                // validar que existan paradas en paradasMap
                int idIni = Integer.parseInt(tramo.getInicio().getCodigo());
                int idFin = Integer.parseInt(tramo.getFin().getCodigo());
                if (!paradasMap.containsKey(idIni) || !paradasMap.containsKey(idFin))
                    throw new InstanciaNoExisteEnBDException("Faltan paradas");
                tramosMap.put(key, tramo);
            }

            @Override
            public void actualizar(Tramo tramo) {
                String key = tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo();
                tramosMap.put(key, tramo);
            }

            @Override
            public void borrar(Tramo tramo) throws InstanciaNoExisteEnBDException {
                String key = tramo.getInicio().getCodigo() + "-" + tramo.getFin().getCodigo();
                if (!tramosMap.containsKey(key)) throw new InstanciaNoExisteEnBDException("No existe tramo");
                tramosMap.remove(key);
            }

            @Override
            public Map<String, Tramo> buscarTodos() {
                return new HashMap<>(tramosMap);
            }
        };

        // Reemplazar por reflexión los campos privados en Empresa
        setPrivateField(empresa, "paradaDAO", paradaDAO);
        setPrivateField(empresa, "lineaDAO", lineaDAO);
        setPrivateField(empresa, "tramoDAO", tramoDAO);

        setPrivateField(empresa, "paradas", paradasMap);
        setPrivateField(empresa, "lineas", lineasMap);
        setPrivateField(empresa, "tramos", tramosMap);

        return empresa;
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
