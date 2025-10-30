package colectivo.modelo;

import java.time.LocalTime;

public class Tramo {

    //atributos
    private int tiempo;
    private int tipo;

    private Parada inicio;
    private Parada fin;


    //Constructor
    public Tramo(int tiempo, int tipo, Parada inicio, Parada fin) {
        this.tiempo = tiempo;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fin = fin;
    }

    //Getters y setters
    public int getTiempo() {
        return tiempo;
    }

    public void setTiempo(int tiempo) {
        this.tiempo = tiempo;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public Parada getInicio() {
        return inicio;
    }

    public void setInicio(Parada inicio) {
        this.inicio = inicio;
    }

    public Parada getFin() {
        return fin;
    }

    public void setFin(Parada fin) {
        this.fin = fin;
    }
}
