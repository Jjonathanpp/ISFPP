package colectivo.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parada {
    private String codigo;
    private String direccion;
    private double latitud;
    private double longitud;
    private List<Linea> lineas;
    private List<Parada> paradasCaminando;

    public Parada(String codigo, String direccion, double latitud, double longitud) {
        this.codigo = codigo;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;

        this.lineas = new ArrayList<>();
        this.paradasCaminando = new ArrayList<>();
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }

    public List<Parada> getParadasCaminando() {
        return paradasCaminando;
    }

    public void addLinea(Linea linea) {
        this.lineas.add(linea);
    }
    public void addParadaCaminando(Parada parada) {
        this.paradasCaminando.add(parada);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parada)) return false;
        Parada parada = (Parada) o;
        return Objects.equals(codigo, parada.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "Parada{" + "codigo='" + codigo + '\'' + ", direccion='" + direccion + '\'' + '}';
    }
}
