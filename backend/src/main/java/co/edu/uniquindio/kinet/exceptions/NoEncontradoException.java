package co.edu.uniquindio.kinet.exceptions;

/** El recurso pedido no existe (404). */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
