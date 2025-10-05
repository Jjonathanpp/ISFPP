package colectivo.modelo;

import java.util.List;

public class Linea {

    //atributos
    private String codigo;
    private String nombre;

    private  List<Recorrido> recorridos;

    private List<Frecuencia> frecuencias;

    private List<Parada> paradas;

    //Constructor
    public Linea(String codigo, String nombre, List<Recorrido> recorridos, List<Frecuencia> frecuencias, List<Parada> paradas) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.recorridos = recorridos;
        this.frecuencias = frecuencias;
        this.paradas = paradas;
    }

    //getters y setters
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Recorrido> getRecorridos() {
        return recorridos;
    }

    public void setRecorridos(List<Recorrido> recorridos) {
        this.recorridos = recorridos;
    }

    public List<Frecuencia> getFrecuencias() {
        return frecuencias;
    }

    public void setFrecuencias(List<Frecuencia> frecuencias) {
        this.frecuencias = frecuencias;
    }

    public List<Parada> getParadas() {
        return paradas;
    }

    public void setParadas(List<Parada> paradas) {
        this.paradas = paradas;
    }
}
