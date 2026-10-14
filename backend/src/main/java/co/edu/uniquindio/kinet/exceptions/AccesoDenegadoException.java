package co.edu.uniquindio.kinet.exceptions;

/** El usuario está autenticado pero no puede hacer esta acción (403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
