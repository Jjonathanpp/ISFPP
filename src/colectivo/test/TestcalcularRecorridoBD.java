package colectivo.test;

import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.logica.Calculo;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

class TestcalcularRecorrido {

    private Map<String, Parada> paradas;
    private Map<String, Tramo> tramos;

    private int diaSemana;
    private LocalTime horaLlegaParada;

    @BeforeEach
    void setUp() {
        ParadaSecuencialDAO paradaDAO = new ParadaSecuencialDAO();
        paradas = paradaDAO.buscarTodos().values().stream()
                .collect(Collectors.toMap(Parada::getCodigo, Function.identity()));
        tramos = new HashMap<>();
        diaSemana = 1; // lunes
        horaLlegaParada = LocalTime.of(10, 35); // hora de llegada a la parad
    }

    @Test
    void testSinColectivo() {
        Parada paradaOrigen = paradas.get("66");
        Parada paradaDestino = paradas.get("31");

        List<List<Recorrido>> recorridos = Calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
                horaLlegaParada, tramos);

        assertTrue(recorridos.isEmpty());
    }

    @Test
    void testDirecto() {
        Parada paradaOrigen = paradas.get("44");
        Parada paradaDestino = paradas.get("47");

        List<List<Recorrido>> recorridos = Calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
                horaLlegaParada, tramos);

        assertEquals(2, recorridos.size());

        Map<String, Recorrido> recorridosPorLinea = recorridos.stream()
                .filter(ruta -> ruta.size() == 1)
                .map(ruta -> ruta.get(0))
                .collect(Collectors.toMap(recorrido -> recorrido.getLinea().getCodigo(), Function.identity()));

        Recorrido recorridoL1I = recorridosPorLinea.get("L1I");
        assertNotNull(recorridoL1I);
        assertEquals(Arrays.asList("44", "43", "47"), codigos(recorridoL1I.getParadas()));
        assertEquals(LocalTime.of(10, 50), recorridoL1I.getHoraSalida());
        assertEquals(180, recorridoL1I.getDuracion());

        Recorrido recorridoL5R = recorridosPorLinea.get("L5R");
        assertNotNull(recorridoL5R);
        assertEquals(Arrays.asList("44", "43", "47"), codigos(recorridoL5R.getParadas()));
        assertEquals(LocalTime.of(10, 47, 30), recorridoL5R.getHoraSalida());
        assertEquals(180, recorridoL5R.getDuracion());

    }

    @Test
    void testConexion() {
        Parada paradaOrigen = paradas.get("88");
        Parada paradaDestino = paradas.get("13");

        List<List<Recorrido>> recorridos = Calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
                horaLlegaParada, tramos);

        assertEquals(2, recorridos.size());
        List<Recorrido> rutaL1I = recorridos.stream()
                .filter(ruta -> !ruta.isEmpty() && "L1I".equals(ruta.get(0).getLinea().getCodigo()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, rutaL1I.size());

        Recorrido primerTramoL1I = rutaL1I.get(0);
        assertEquals("L1I", primerTramoL1I.getLinea().getCodigo());
        assertEquals(Arrays.asList("88", "97", "44"), codigos(primerTramoL1I.getParadas()));
        assertEquals(LocalTime.of(10, 48), primerTramoL1I.getHoraSalida());
        assertEquals(120, primerTramoL1I.getDuracion());

        Recorrido segundoTramoL1I = rutaL1I.get(1);
        assertEquals("L5R", segundoTramoL1I.getLinea().getCodigo());
        assertEquals(Arrays.asList("44", "43", "47", "99", "24", "5", "54", "28", "101", "18", "78", "13"),
                codigos(segundoTramoL1I.getParadas()));
        assertEquals(LocalTime.of(11, 7, 30), segundoTramoL1I.getHoraSalida());
        assertEquals(1110, segundoTramoL1I.getDuracion());

        List<Recorrido> rutaL4R = recorridos.stream()
                .filter(ruta -> !ruta.isEmpty() && "L4R".equals(ruta.get(0).getLinea().getCodigo()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, rutaL4R.size());

        Recorrido primerTramoL4R = rutaL4R.get(0);
        assertEquals("L4R", primerTramoL4R.getLinea().getCodigo());
        assertEquals(Arrays.asList("88", "63", "65", "64", "77", "25", "5"), codigos(primerTramoL4R.getParadas()));
        assertEquals(LocalTime.of(10, 36), primerTramoL4R.getHoraSalida());
        assertEquals(720, primerTramoL4R.getDuracion());

        Recorrido segundoTramoL4R = rutaL4R.get(1);
        assertEquals("L5R", segundoTramoL4R.getLinea().getCodigo());
        assertEquals(Arrays.asList("5", "54", "28", "101", "18", "78", "13"), codigos(segundoTramoL4R.getParadas()));
        assertEquals(LocalTime.of(10, 55), segundoTramoL4R.getHoraSalida());
        assertEquals(660, segundoTramoL4R.getDuracion());
    }

    @Test
    void testConexionCaminando() {
        Parada paradaOrigen = paradas.get("31");
        Parada paradaDestino = paradas.get("66");

        List<List<Recorrido>> recorridos = Calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
                horaLlegaParada, tramos);

        assertEquals(1, recorridos.size());
        List<Recorrido> ruta = recorridos.get(0);
        assertEquals(3, ruta.size());

        Recorrido primerTramo = ruta.get(0);
        assertEquals("L2R", primerTramo.getLinea().getCodigo());
        assertEquals(Arrays.asList("31", "8", "33", "20", "25", "24"), codigos(primerTramo.getParadas()));
        assertEquals(LocalTime.of(10, 39), primerTramo.getHoraSalida());
        assertEquals(480, primerTramo.getDuracion());

        Recorrido tramoCaminando = ruta.get(1);
        assertNull(tramoCaminando.getLinea());
        assertEquals(Arrays.asList("24", "75"), codigos(tramoCaminando.getParadas()));
        assertEquals(LocalTime.of(10, 47), tramoCaminando.getHoraSalida());
        assertEquals(120, tramoCaminando.getDuracion());

        Recorrido tercerTramo = ruta.get(2);
        assertEquals("L6I", tercerTramo.getLinea().getCodigo());
        assertEquals(Arrays.asList("75", "76", "38", "40", "66"), codigos(tercerTramo.getParadas()));
        assertEquals(LocalTime.of(11, 2), tercerTramo.getHoraSalida());
        assertEquals(600, tercerTramo.getDuracion());
    }
    private List<String> codigos(List<Parada> paradasRecorrido) {
        return paradasRecorrido.stream().map(Parada::getCodigo).toList();
    }
}
