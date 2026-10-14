package co.edu.uniquindio.kinet.exceptions;

/** La petición es válida pero rompe una regla del juego (400), por ejemplo unirse a un reto de otro nivel. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
