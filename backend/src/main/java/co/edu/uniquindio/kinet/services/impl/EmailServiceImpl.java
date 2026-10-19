package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía el correo de recuperación. Si no hay servidor de correo configurado (MAIL_HOST vacío),
 * escribe el enlace en la consola para poder probar en local sin enviar correos.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String remitente;
    private final String urlFrontend;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${kinet.correo.remitente}") String remitente,
                            @Value("${kinet.frontend.url}") String urlFrontend) {
        this.mailSender = mailSender;
        this.host = host;
        this.remitente = remitente;
        this.urlFrontend = urlFrontend;
    }

    @Override
    public void enviarRecuperacion(String correo, String nombre, String token) {
        String enlace = urlFrontend + "/restablecer-contrasena?token=" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (host == null || host.isBlank() || sender == null) {
            log.info("[CORREO SIMULADO] Recuperación de contraseña para {}: {}", correo, enlace);
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correo);
        mensaje.setSubject("Kinet: restablece tu contraseña");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña de Kinet.\n"
                + "Abre este enlace para crear una nueva (vence en 30 minutos):\n\n"
                + enlace + "\n\n"
                + "Si no fuiste tú, ignora este correo.\n\nEquipo Kinet");
        try {
            sender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}. Enlace: {}", correo, e.getMessage(), enlace);
        }
    }

    @Override
    public void enviarAviso(String correo, String asunto, String mensaje) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (host == null || host.isBlank() || sender == null) {
            log.info("[CORREO SIMULADO] Para {}: {} - {}", correo, asunto, mensaje);
            return;
        }
        SimpleMailMessage correoAviso = new SimpleMailMessage();
        correoAviso.setFrom(remitente);
        correoAviso.setTo(correo);
        correoAviso.setSubject("Kinet: " + asunto);
        correoAviso.setText(mensaje + "\n\nEquipo Kinet");
        sender.send(correoAviso);
    }
}
