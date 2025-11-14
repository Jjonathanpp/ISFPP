package colectivo.aplicacion;

import org.apache.log4j.Logger;

public class AplicacionPrincipal {

    private static final Logger LOGGER = Logger.getLogger(AplicacionPrincipal.class);
    private final Coordinador coordinador;

    public AplicacionPrincipal() {
        this.coordinador = new Coordinador();
    }

    public void iniciar() {
        try {
            coordinador.inicializarInterfaz();
        } catch (Exception e) {
            LOGGER.fatal("Error crítico al iniciar la aplicación", e);
            throw new IllegalStateException("No fue posible iniciar la aplicación", e);
        }
    }

    public Coordinador getCoordinador() {
        return coordinador;
    }
}
