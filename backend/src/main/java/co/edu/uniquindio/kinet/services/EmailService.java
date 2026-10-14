package co.edu.uniquindio.kinet.services;

/** Envío de correos transaccionales (RNF-08). */
public interface EmailService {

    void enviarRecuperacion(String correo, String nombre, String token);
}
