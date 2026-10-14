package co.edu.uniquindio.kinet.exceptions;

/** Correo o contraseña incorrectos, o código de recuperación inválido (401). */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
