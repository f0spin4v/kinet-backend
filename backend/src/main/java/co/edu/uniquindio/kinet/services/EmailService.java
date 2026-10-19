package co.edu.uniquindio.kinet.services;

/** Envío de correos transaccionales (RNF-08). */
public interface EmailService {

    void enviarRecuperacion(String correo, String nombre, String token);

    /** Aviso del canal EMAIL de las notificaciones. Lanza excepción si el envío falla. */
    void enviarAviso(String correo, String asunto, String mensaje);
}
