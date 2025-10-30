package colectivo.excepciones;

public class InstanciaNoExisteEnBDException extends Exception {
    public InstanciaNoExisteEnBDException(String mensaje) {
        super(mensaje);
    }
}
