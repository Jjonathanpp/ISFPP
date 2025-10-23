package colectivo.test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colectivo.conexion.Factory;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.logica.Calculo;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;

class TestCalcularRecorridoDAO {

	private Map<Integer, Parada> paradas;
	private Map<String, Linea> lineas;
	private Map<String, Tramo> tramos;

	private int diaSemana;
	private LocalTime horaLlegaParada;
	
	private Calculo calculo;	

	@BeforeEach
	void setUp() throws Exception {

		paradas = ((ParadaDAO) Factory.getInstancia("PARADA")).buscarTodos();

		tramos = ((TramoDAO) Factory.getInstancia("TRAMO")).buscarTodos();
		
		lineas = ((LineaDAO) Factory.getInstancia("LINEA")).buscarTodos();

		diaSemana = 1; // lunes
		horaLlegaParada = LocalTime.of(10, 35);

		calculo = new Calculo();
	}

	@Test
	void testSinColectivo() {
		Parada paradaOrigen = paradas.get(66);
		Parada paradaDestino = paradas.get(31);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertTrue(recorridos.isEmpty());
	}

	@Test
	void testDirecto() {
		Parada paradaOrigen = paradas.get(44);
		Parada paradaDestino = paradas.get(47);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertEquals(2, recorridos.size());
		assertEquals(1, recorridos.get(0).size());
		assertEquals(1, recorridos.get(1).size());

		Recorrido recorrido1;
		Recorrido recorrido2;
		if (recorridos.get(0).get(0).getLinea().equals(lineas.get("L1I"))) {
			recorrido1 = recorridos.get(0).get(0);
			recorrido2 = recorridos.get(1).get(0);
		} else {
			recorrido1 = recorridos.get(0).get(1);
			recorrido2 = recorridos.get(0).get(0);
		}

		// recorrido1
		assertEquals(lineas.get("L1I"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(44));
		paradas1.add(paradas.get(43));
		paradas1.add(paradas.get(47));
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(10, 50), recorrido1.getHoraSalida());
		assertEquals(180, recorrido1.getDuracion());

		// recorrido2
		assertEquals(lineas.get("L5R"), recorrido2.getLinea());
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(44));
		paradas2.add(paradas.get(43));
		paradas2.add(paradas.get(47));
		assertIterableEquals(paradas2, recorrido2.getParadas());
		assertEquals(LocalTime.of(10, 47, 30), recorrido2.getHoraSalida());
		assertEquals(180, recorrido2.getDuracion());

	}

	@Test
	void testConexion() {
		Parada paradaOrigen = paradas.get(88);
		Parada paradaDestino = paradas.get(13);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);

		assertEquals(2, recorridos.size());
		assertEquals(2, recorridos.get(0).size());
		assertEquals(2, recorridos.get(1).size());

		Recorrido recorrido1;
		Recorrido recorrido2;
		Recorrido recorrido3;
		Recorrido recorrido4;
		if (recorridos.get(0).get(0).getLinea().equals(lineas.get("L1I"))) {
			recorrido1 = recorridos.get(0).get(0);
			recorrido2 = recorridos.get(0).get(1);
			recorrido3 = recorridos.get(1).get(0);
			recorrido4 = recorridos.get(1).get(1);
		} else {
			recorrido1 = recorridos.get(1).get(0);
			recorrido2 = recorridos.get(1).get(1);
			recorrido3 = recorridos.get(0).get(0);
			recorrido4 = recorridos.get(0).get(1);
		}

		// recorrido1
		assertEquals(lineas.get("L1I"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(88));
		paradas1.add(paradas.get(97));
		paradas1.add(paradas.get(44));
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(10, 48), recorrido1.getHoraSalida());
		assertEquals(120, recorrido1.getDuracion());

		// recorrido2
		assertEquals(lineas.get("L5R"), recorrido2.getLinea());
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(44));
		paradas2.add(paradas.get(43));
		paradas2.add(paradas.get(47));
		paradas2.add(paradas.get(99));
		paradas2.add(paradas.get(24));
		paradas2.add(paradas.get(5));
		paradas2.add(paradas.get(54));
		paradas2.add(paradas.get(28));
		paradas2.add(paradas.get(101));
		paradas2.add(paradas.get(18));
		paradas2.add(paradas.get(78));
		paradas2.add(paradas.get(13));
		assertIterableEquals(paradas2, recorrido2.getParadas());
		assertEquals(LocalTime.of(11, 07, 30), recorrido2.getHoraSalida());
		assertEquals(1110, recorrido2.getDuracion());

		// recorrido3
		assertEquals(lineas.get("L4R"), recorrido3.getLinea());
		List<Parada> paradas3 = new ArrayList<Parada>();
		paradas3.add(paradas.get(88));
		paradas3.add(paradas.get(63));
		paradas3.add(paradas.get(65));
		paradas3.add(paradas.get(64));
		paradas3.add(paradas.get(77));
		paradas3.add(paradas.get(25));
		paradas3.add(paradas.get(5));
		assertIterableEquals(paradas3, recorrido3.getParadas());
		assertEquals(LocalTime.of(10, 36), recorrido3.getHoraSalida());
		assertEquals(720, recorrido3.getDuracion());

		// recorrido4
		assertEquals(lineas.get("L5R"), recorrido4.getLinea());
		List<Parada> paradas4 = new ArrayList<Parada>();
		paradas4.add(paradas.get(5));
		paradas4.add(paradas.get(54));
		paradas4.add(paradas.get(28));
		paradas4.add(paradas.get(101));
		paradas4.add(paradas.get(18));
		paradas4.add(paradas.get(78));
		paradas4.add(paradas.get(13));
		assertIterableEquals(paradas4, recorrido4.getParadas());
		assertEquals(LocalTime.of(10, 55), recorrido4.getHoraSalida());
		assertEquals(660, recorrido4.getDuracion());
	}

	@Test
	void testConexionCaminando() {
		Parada paradaOrigen = paradas.get(31);
		Parada paradaDestino = paradas.get(66);

		List<List<Recorrido>> recorridos = calculo.calcularRecorrido(paradaOrigen, paradaDestino, diaSemana,
				horaLlegaParada, tramos);
		
		assertEquals(1, recorridos.size());
		assertEquals(3, recorridos.get(0).size());		

		Recorrido recorrido1 = recorridos.get(0).get(0);
		Recorrido recorrido2 = recorridos.get(0).get(1);
		Recorrido recorrido3 = recorridos.get(0).get(2);
		
		// recorrido1
		assertEquals(lineas.get("L2R"), recorrido1.getLinea());
		List<Parada> paradas1 = new ArrayList<Parada>();
		paradas1.add(paradas.get(31));
		paradas1.add(paradas.get(8));
		paradas1.add(paradas.get(33));
		paradas1.add(paradas.get(20));
		paradas1.add(paradas.get(25));
		paradas1.add(paradas.get(24));		
		assertIterableEquals(paradas1, recorrido1.getParadas());
		assertEquals(LocalTime.of(10, 39), recorrido1.getHoraSalida());
		assertEquals(480, recorrido1.getDuracion());
		
		// recorrido2
		assertNull(recorrido2.getLinea()); // Caminando
		List<Parada> paradas2 = new ArrayList<Parada>();
		paradas2.add(paradas.get(24));
		paradas2.add(paradas.get(75));		
		assertIterableEquals(paradas2, recorrido2.getParadas());
		assertEquals(LocalTime.of(10, 47), recorrido2.getHoraSalida());
		assertEquals(120, recorrido2.getDuracion());

		// recorrido3
		assertEquals(lineas.get("L6I"), recorrido3.getLinea());
		List<Parada> paradas3 = new ArrayList<Parada>();
		paradas3.add(paradas.get(75));
		paradas3.add(paradas.get(76));
		paradas3.add(paradas.get(38));
		paradas3.add(paradas.get(40));
		paradas3.add(paradas.get(66));		
		assertIterableEquals(paradas3, recorrido3.getParadas());
		assertEquals(LocalTime.of(11, 02), recorrido3.getHoraSalida());
		assertEquals(600, recorrido3.getDuracion());

	}
}
