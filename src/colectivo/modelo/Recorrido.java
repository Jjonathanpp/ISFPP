package colectivo.modelo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Recorrido {
    //Atributos
    private LocalTime horaSalida;
    private int duracion; // duración en segundos
    private Linea linea;
    private List<Parada> paradas;

    //Constructor
    public Recorrido(LocalTime horaSalida, Linea linea, Parada p1, Parada p2) {
        this.horaSalida = horaSalida;
        this.linea = linea;
        this.paradas = new ArrayList<>();
        this.paradas.add(p1);
        this.paradas.add(p2);
        this.duracion = 0;
    }

    //Getters y setters
    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
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