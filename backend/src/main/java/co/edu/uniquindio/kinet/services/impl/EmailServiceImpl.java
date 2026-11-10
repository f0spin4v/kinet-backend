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
    public void enviarAutorizacionMenor(String correoRepresentante, String nombreRepresentante, String nombreMenor,
                                        String token) {
        String enlace = urlFrontend + "/autorizacion-representante?token=" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (host == null || host.isBlank() || sender == null) {
            log.info("[CORREO SIMULADO] Autorización del representante {}: {}", correoRepresentante, enlace);
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correoRepresentante);
        mensaje.setSubject("Kinet: " + nombreMenor + " necesita tu autorización");
        mensaje.setText("Hola " + nombreRepresentante + ",\n\n"
                + nombreMenor + " se registró en Kinet, una app que convierte la actividad física en un juego, y te "
                + "indicó como su representante legal.\n\n"
                + "Como es menor de 18 años, la ley colombiana (Ley 1581 de 2012 y Decreto 1377 de 2013, art. 12) "
                + "exige tu autorización para tratar sus datos. Su cuenta no se puede usar hasta que respondas.\n\n"
                + "Abre este enlace para leer qué datos tratamos y autorizar o no (vence en 7 días):\n\n"
                + enlace + "\n\n"
                + "Si no conoces a esta persona o no eres su representante legal, ignora este correo.\n\nEquipo Kinet");
        try {
            sender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}. Enlace: {}", correoRepresentante, e.getMessage(), enlace);
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
