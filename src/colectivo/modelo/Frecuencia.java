package colectivo.modelo;

import java.time.LocalTime;

public class Frecuencia {

    //Atributos
    private int diaSemana;
    private LocalTime hora;

    private Linea linea;

    //Constructor
    public Frecuencia(int diaSemana, LocalTime hora, Linea linea) {
        this.diaSemana = diaSemana;
        this.hora = hora;
        this.linea = linea;
    }

    //getters y setters
    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public Linea getLinea() {
        return linea;
    }

    public void setLinea(Linea linea) {
        this.linea = linea;
    }
}
