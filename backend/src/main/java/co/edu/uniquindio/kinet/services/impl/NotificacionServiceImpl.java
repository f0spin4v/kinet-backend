package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.mappers.NotificacionMapper;
import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Guarda cada aviso en la bandeja del jugador. El envío al proveedor externo de push (ADR-06) está simulado:
 * se registra en el log, listo para reemplazarse por el servicio real.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionMapper notificacionMapper;

    @Override
    @Transactional
    public void notificar(Usuario usuario, TipoNotificacion tipo, String mensaje) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .mensaje(mensaje.length() > 300 ? mensaje.substring(0, 300) : mensaje)
                .build());
        log.info("[PUSH simulado] para {}: {}", usuario.getCorreo(), mensaje);
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
