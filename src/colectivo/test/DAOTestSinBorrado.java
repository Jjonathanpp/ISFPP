package colectivo.test;

import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.dao.secuencial.LineaSecuencialDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.dao.secuencial.TramoSecuencialDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DAOTestSinBorrado {
    @Test
    public void testInsertarYLeerParada() {
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        Parada parada = new Parada("9999", "Test Sin Borrar", -99.0, -99.0);
        paradaDAO.insertar(parada);

        Map<Integer, Parada> todas = paradaDAO.buscarTodos();
        boolean encontrada = todas.values().stream().anyMatch(p -> "9999".equals(p.getCodigo()));
        assertTrue(encontrada, "La parada con código 9999 debería estar en la base de datos");
    }

    @Test
    public void testInsertarYLeerTramo() {
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        TramoDAO tramoDAO = new TramoSecuencialDAO();

        Parada inicio = new Parada("8888", "Inicio Test", -88.0, -88.0);
        Parada fin = new Parada("8889", "Fin Test", -88.1, -88.1);
        paradaDAO.insertar(inicio);
        paradaDAO.insertar(fin);

        Tramo tramo = new Tramo(15, 2, inicio, fin);
        tramoDAO.insertar(tramo);

        Map<String, Tramo> tramos = tramoDAO.buscarTodos();
        boolean encontrado = tramos.containsKey("8888-8889");
        assertTrue(encontrado, "El tramo 8888-8889 debería estar en la base de datos");
    }

    @Test
    public void testInsertarYLeerLinea() {
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        LineaDAO lineaDAO = new LineaSecuencialDAO();

        Parada p1 = new Parada("7777", "Parada 1 Test", -77.0, -77.0);
        Parada p2 = new Parada("7778", "Parada 2 Test", -77.1, -77.1);
        paradaDAO.insertar(p1);
        paradaDAO.insertar(p2);

        List<Parada> paradas = new ArrayList<>();
        paradas.add(p1);
        paradas.add(p2);

        Linea linea = new Linea("LINTEST", "Linea Test", paradas);

        lineaDAO.insertar(linea);

        Map<String, Linea> lineas = lineaDAO.buscarTodos();
        boolean encontrada = lineas.containsKey("LINTEST");
        assertTrue(encontrada, "La línea LINTEST debería estar en la base de datos");
    }

}
