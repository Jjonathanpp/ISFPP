package colectivo.test;

import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.dao.secuencial.TramoSecuencialDAO;
import colectivo.modelo.Tramo;

import java.util.Map;

public class TestTramoDAO {
    public static void main(String[] args) {
        // Instanciamos los DAOs
        ParadaDAO paradaDAO = new ParadaSecuencialDAO();
        TramoDAO tramoDAO = new TramoSecuencialDAO();

        // Ejecutamos buscarTodos() en tramoDAO
        Map<String, Tramo> tramos = tramoDAO.buscarTodos();

        System.out.println("Cantidad de tramos encontrados: " + tramos.size());

        for (Map.Entry<String, Tramo> entry : tramos.entrySet()) {
            String clave = entry.getKey();
            Tramo tramo = entry.getValue();

            System.out.println("Clave: " + clave +
                    " → Tiempo: " + tramo.getTiempo() +
                    "s, Tipo: " + (tramo.getTipo() == 1 ? "Colectivo" : "Caminando") +
                    ", Inicio: " + tramo.getInicio().getCodigo() +
                    ", Fin: " + tramo.getFin().getCodigo());
        }
    }
}

