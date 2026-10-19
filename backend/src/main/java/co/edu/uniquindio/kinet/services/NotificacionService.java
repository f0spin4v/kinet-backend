package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
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
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    /** Aviso por push. Regresa de inmediato; si la transacción actual se deshace, el aviso no se crea. */
    public void notificar(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje) {
        notificar(usuario, tipo, CanalNotificacion.PUSH, titulo, mensaje);
    }

    public void notificar(Usuario usuario, TipoNotificacion tipo, CanalNotificacion canal, String titulo,
                          String mensaje) {
        eventos.publishEvent(new NotificacionEvento(usuario.getId(), usuario.getCorreo(), tipo, canal,
                recortar(titulo, 100), recortar(mensaje, 300), LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public PaginaDTO<NotificacionDTO> listar(String correo, Pageable pagina) {
        return PaginaDTO.de(notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDescIdDesc(idDe(correo), pagina).map(NotificacionDTO::de));
    }

    @Transactional
    public NotificacionDTO marcarLeida(String correo, Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(idNotificacion, idDe(correo))
                .orElseThrow(() -> new NoEncontradoException("La notificación no existe."));
        if (notificacion.getFechaLectura() == null) {
            notificacion.setFechaLectura(LocalDateTime.now());
        }
        return NotificacionDTO.de(notificacionRepository.save(notificacion));
    }

    /** Marca toda la bandeja como leída y devuelve cuántas cambiaron. */
    @Transactional
    public int marcarTodasLeidas(String correo) {
        return notificacionRepository.marcarTodas(idDe(correo), LocalDateTime.now());
    }

    /** Borra una notificación de mi bandeja: deja de aparecer. */
    @Transactional
    public void eliminar(String correo, Long idNotificacion) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(idNotificacion, idDe(correo))
                .orElseThrow(() -> new NoEncontradoException("La notificación no existe."));
        notificacionRepository.delete(notificacion);
    }

    /** Vacía mi bandeja y devuelve cuántas borró. */
    @Transactional
    public int eliminarTodas(String correo) {
        return notificacionRepository.borrarTodas(idDe(correo));
    }

    private Long idDe(String correo) {
        return usuarioRepository.idPorCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."));
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }
}
