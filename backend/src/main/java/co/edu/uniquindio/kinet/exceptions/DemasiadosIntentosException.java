package co.edu.uniquindio.kinet.exceptions;

/** Demasiados intentos fallidos de inicio de sesión en poco tiempo (429). */
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
