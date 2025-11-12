package colectivo.modelo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Recorrido {
    private LocalTime horaSalida;
    private int duracion;
    private Linea linea;
    private List<Parada> paradas;

    public Recorrido(LocalTime horaSalida, Linea linea, Parada p1, Parada p2) {
        this.horaSalida = horaSalida;
        this.linea = linea;
        this.paradas = new ArrayList<>();
        this.paradas.add(p1);
        this.paradas.add(p2);
        this.duracion = 0;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
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

    public void agregarParada(Parada parada) {
        this.paradas.add(parada);
    }
}