package colectivo.aplicacion;

import colectivo.conexion.Factory;
import colectivo.interfaz.VistaInterfaz;
import colectivo.logica.Calculo;
import colectivo.logica.Empresa;
import colectivo.modelo.Parada;
import org.apache.log4j.Logger;

import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

public class Coordinador {
    private static final Logger LOGGER = Logger.getLogger(Coordinador.class);
    private final VistaInterfaz vista;
    private final Empresa empresa;

    public Coordinador() {
        this(Empresa.getEmpresa());
    }

    public Coordinador(Empresa empresa) {
        this.vista = Factory.getInstancia("UI", VistaInterfaz.class);
        this.vista.setCoordinador(this);
        this.empresa = Objects.requireNonNull(empresa, "La empresa no puede ser nula");
    }

    public void inicializarInterfaz() {
        vista.construirVista();
        vista.inicializar();
    }

    public VistaInterfaz getVista() {
        return vista;
    }

    public void calcularRecorrido(Parada origen, Parada destino, int dia, LocalTime hora) {
        LOGGER.debug("Invocando calcularRecorrido con parámetros: dia=" + dia + ", hora=" + hora);
        vista.mostrarRecorridos(
                Calculo.calcularRecorrido(origen, destino, dia, hora, empresa.getTramos()),
                origen,
                destino,
                hora
        );
    }


    public Map<Integer, Parada> listarParadas() {
        return empresa.getParadas();
    }
}
