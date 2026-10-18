package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.mappers.NotificacionMapper;
import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.services.NotificacionEvento;
import co.edu.uniquindio.kinet.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Bandeja de avisos del jugador. Crear un aviso solo publica un evento: lo guarda y lo envía
 * {@link NotificacionListener} en otro hilo, después de que se confirme la operación que lo originó.
 */
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionMapper notificacionMapper;
    private final ApplicationEventPublisher eventos;

    @Override
    public void notificar(Usuario usuario, TipoNotificacion tipo, String mensaje) {
        eventos.publishEvent(new NotificacionEvento(
                usuario.getId(),
                usuario.getCorreo(),
                tipo,
                mensaje.length() > 300 ? mensaje.substring(0, 300) : mensaje,
                LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> listar(String correo) {
        return notificacionRepository.findTop30ByUsuarioIdOrderByFechaDesc(idDe(correo)).stream()
                .map(notificacionMapper::toNotificacionDTO)
                .toList();
    }

    @Override
    @Transactional
    public NotificacionDTO marcarLeida(String correo, Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(idNotificacion, idDe(correo))
                .orElseThrow(() -> new NoEncontradoException("La notificación no existe."));
        notificacion.setLeida(true);
        return notificacionMapper.toNotificacionDTO(notificacionRepository.save(notificacion));
    }

    private Long idDe(String correo) {
        return usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."))
                .getId();
    }
}
