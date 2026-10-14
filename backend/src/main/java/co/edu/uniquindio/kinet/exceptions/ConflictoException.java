package co.edu.uniquindio.kinet.exceptions;

/** El dato ya existe o choca con otro registro (409), por ejemplo un correo repetido. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
