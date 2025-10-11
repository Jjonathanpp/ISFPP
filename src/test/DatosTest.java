package test;

import colectivo.datos.CargaParametros;
import colectivo.datos.Dato;
import colectivo.modelo.Frecuencia;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class DatosTest {

    private static TreeMap<String, Linea> lineas;
    private static TreeMap<String, Frecuencia> frecuencias;
    private static TreeMap<String, Parada> paradas;
    private static TreeMap<String, Tramo> tramos;

    @BeforeEach
    public void setUp() {
        //Cargar parametros:
        try{
            CargaParametros.parametros();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        //Cargar datos
        try{
            paradas = Dato.cargarParadas(CargaParametros.getArchivoParada());
            lineas = Dato.cargarLineas(CargaParametros.getArchivoLinea());
            frecuencias = Dato.cargarFrecuencias(CargaParametros.getArchivoFrecuencia());
            tramos = Dato.cargarTramos(CargaParametros.getArchivoTramo());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    @Test
    void testpParametrosLoades(){
        assertEquals("tramo_PM.txt", CargaParametros.getArchivoTramo());
        assertEquals("linea_PM.txt", CargaParametros.getArchivoLinea());
        assertEquals("parada_PM.txt", CargaParametros.getArchivoParada());
        assertEquals("frecuencia_PM.txt", CargaParametros.getArchivoFrecuencia());
    }

    @Test
    void testParadasLoaded() {
        assertNotNull(paradas);
        assertFalse(paradas.isEmpty());
        //assertEquals(104, paradas.size());
    }
    @Test
    void testLineasLoaded() {
        assertNotNull(lineas);
        assertFalse(lineas.isEmpty());
        //assertEquals(17, lineas.size());
    }
    @Test
    void testTramosLoaded() {
        assertNotNull(tramos);
        assertFalse(tramos.isEmpty());
        //assertEquals(49, tramos.size());
    }
    @Test
    void testFrecuenciasLoaded() {
        assertNotNull(frecuencias);
        assertFalse(frecuencias.isEmpty());
        //assertEquals(17, frecuencias.size());
    }
}
