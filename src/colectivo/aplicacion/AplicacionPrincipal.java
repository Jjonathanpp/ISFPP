package colectivo.aplicacion;

import colectivo.logica.Empresa;

public class AplicacionPrincipal {

    private final Coordinador coordinador;

    public AplicacionPrincipal() {
        this.coordinador = new Coordinador();
    }

    public void iniciar() {
        coordinador.setEmpresa(Empresa.getEmpresa());

        coordinador.construirVistaConControler();

        coordinador.getControlerMVC().inicializar();
    }

    public Coordinador getCoordinador() {
        return coordinador;
    }
}
