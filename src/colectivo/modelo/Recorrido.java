package colectivo.modelo;

import java.time.LocalTime;
import java.util.List;

public class Recorrido {
    //Atributos
    private LocalTime horaSalida;
    private int duracion;

    private Linea linea;

    private List<Parada> paradas;

    //Constructor
    public Recorrido(LocalTime horaSalida, int duracion, Linea linea, List<Parada> paradas) {
        this.horaSalida = horaSalida;
        this.duracion = duracion;
        this.linea = linea;
        this.paradas = paradas;
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
}
