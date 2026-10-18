package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.config.AsyncConfig;
import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.services.NotificacionEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Guarda y envía las notificaciones fuera del hilo de la petición.
 * Solo corre si la operación que originó el aviso se confirmó (AFTER_COMMIT): si se deshace, no hay aviso.
 * El envío al proveedor externo de push (ADR-06) está simulado en el log, listo para reemplazarse por el real.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionListener {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Async(AsyncConfig.EJECUTOR_NOTIFICACIONES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesar(NotificacionEvento evento) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(usuarioRepository.getReferenceById(evento.idUsuario()))
                .tipo(evento.tipo())
                .mensaje(evento.mensaje())
                .fecha(evento.fecha())
                .build());
        log.info("[PUSH simulado] para {}: {}", evento.correo(), evento.mensaje());
    }
}
