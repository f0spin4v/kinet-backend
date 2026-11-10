package co.edu.uniquindio.kinet.services;

/** Envío de correos transaccionales (RNF-08). */
public interface EmailService {

    void enviarRecuperacion(String correo, String nombre, String token);

    /** Enlace para que el representante legal autorice (o no) la cuenta de un titular de 14 a 17 años (RN-33). */
    void enviarAutorizacionMenor(String correoRepresentante, String nombreRepresentante, String nombreMenor, String token);

    /** Aviso del canal EMAIL de las notificaciones. Lanza excepción si el envío falla. */
    void enviarAviso(String correo, String asunto, String mensaje);
}
