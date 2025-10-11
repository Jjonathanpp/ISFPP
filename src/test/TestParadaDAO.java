package test;

import colectivo.dao.ParadaDAO;
import colectivo.dao.secuencial.ParadaSecuencialDAO;
import colectivo.modelo.Parada;

import java.util.Map;

public class TestParadaDAO {
    public static void main(String[] args) {
        ParadaDAO dao = new ParadaSecuencialDAO();
        Map<Integer, Parada> paradas = dao.buscarTodos();

        System.out.println("Cantidad de paradas encontradas: " + paradas.size());
        for (Map.Entry<Integer, Parada> entry : paradas.entrySet()) {
            Integer clave = entry.getKey();
            Parada p = entry.getValue();
            System.out.println("Clave: " + clave + " → Parada: " + p.getCodigo() + " - " + p.getDireccion());
        }
    }
}

