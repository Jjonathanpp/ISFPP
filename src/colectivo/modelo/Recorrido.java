package colectivo.modelo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Recorrido {
    //Atributos
    private LocalTime horaSalida;

    private Linea linea;

    private List<Parada> paradas;

    //Constructor
    //Cambie el constructor para que reciba dos paradas y las agregue a la lista de paradas
    //Hay que ver que estructura de datos es la mas conveniente para implementar la lista de paradas
    //Por ahora use un ArrayList
    public Recorrido(LocalTime horaSalida, Linea linea, Parada p1, Parada p2) {
        this.horaSalida = horaSalida;
        this.linea = linea;
        this.paradas = new ArrayList<>();
        this.paradas.add(p1);
        this.paradas.add(p2);
    }

    //Getters y setters
    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Linea getLinea() {
        return linea;
    }

    public void setLinea(Linea linea) {
        this.linea = linea;
    }

    public List<Parada> getParadas() {
        return paradas;
    }

    public void setParadas(List<Parada> paradas) {
        this.paradas = paradas;
    }

    //Metodo para agregar una parada al recorrido
    public void agregarParada(Parada parada) {
        this.paradas.add(parada);
    }
}
