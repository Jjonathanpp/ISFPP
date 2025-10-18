package colectivo.excepciones;

public class InstanciaExisteEnBDException extends Exception {
    public InstanciaExisteEnBDException(String mensaje) {
        super(mensaje);
    }
}
