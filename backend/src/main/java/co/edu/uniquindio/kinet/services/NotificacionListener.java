package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.config.AsyncConfig;
import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoEnvio;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * Guarda y envía las notificaciones fuera del hilo de la petición, y solo si la operación que originó el aviso
 * se confirmó (AFTER_COMMIT). Primero se guarda PENDIENTE y después se intenta el envío: si el proceso se cae a
 * mitad de camino, el aviso queda PENDIENTE y no se pierde. El push al proveedor externo (ADR-06) está simulado
 * en el log; el canal EMAIL usa EmailService.
 * Cada paso usa una transacción nueva (REQUIRES_NEW): después del commit la transacción original sigue atada al
 * hilo y lo que se guardara "dentro" de ella no se confirmaría (pasa cuando el ejecutor es síncrono, en pruebas).
 */
@Slf4j
@Component
public class NotificacionListener {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final TransactionTemplate nuevaTransaccion;

    public NotificacionListener(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository,
                                EmailService emailService, PlatformTransactionManager transacciones) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.nuevaTransaccion = new TransactionTemplate(transacciones);
        this.nuevaTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async(AsyncConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void procesar(NotificacionEvento evento) {
        Notificacion notificacion = nuevaTransaccion.execute(estado -> notificacionRepository.save(Notificacion.builder()
                .usuario(usuarioRepository.getReferenceById(evento.idUsuario()))
                .tipo(evento.tipo())
                .canal(evento.canal())
                .titulo(evento.titulo())
                .mensaje(evento.mensaje())
                .fechaCreacion(evento.fecha())
                .build()));
        EstadoEnvio resultado;
        try {
            if (evento.canal() == CanalNotificacion.EMAIL) {
                emailService.enviarAviso(evento.correo(), evento.titulo(), evento.mensaje());
            } else {
                log.info("[PUSH simulado] para {}: {} - {}", evento.correo(), evento.titulo(), evento.mensaje());
            }
            resultado = EstadoEnvio.ENVIADA;
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar la notificación {} a {}: {}", notificacion.getId(), evento.correo(), e.getMessage());
            resultado = EstadoEnvio.FALLIDA;
        }
        EstadoEnvio estadoFinal = resultado;
        nuevaTransaccion.executeWithoutResult(estado ->
                notificacionRepository.marcarEnvio(notificacion.getId(), estadoFinal, LocalDateTime.now()));
    }
}
