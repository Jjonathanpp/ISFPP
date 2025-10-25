package colectivo.modelo;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Linea {

    //atributos
    private String codigo;
    private String nombre;

    private List<Frecuencia> frecuencias;

    private List<Parada> paradas;

    //Constructor
    private static final Logger LOGGER = Logger.getLogger(Linea.class);

    public Linea(String codigo, String nombre, List<Parada> paradas) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.frecuencias = new ArrayList<>();

        if(paradas == null || paradas.size() < 2) {
            LOGGER.error("Intento de crear la línea " + codigo + " con una cantidad inválida de paradas");
            throw new IllegalArgumentException("Una línea debe tener al menos dos paradas");
        }

        this.paradas = new ArrayList<>(paradas);
        LOGGER.debug("Línea " + codigo + " inicializada con " + paradas.size() + " paradas");
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

    //metodos
    public void agregarFrecuencia(Frecuencia frecuencia) {
        this.frecuencias.add(frecuencia);
    }
    public void agregarParada(Parada parada) {
        this.paradas.add(parada);
    }

    // equals/hashCode basados en 'codigo' (identificador lógico)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Linea)) return false;
        Linea linea = (Linea) o;
        return Objects.equals(codigo, linea.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "Linea{" + "codigo='" + codigo + '\'' + ", nombre='" + nombre + '\'' + '}';
    }
}
