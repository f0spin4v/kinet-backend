package co.edu.uniquindio.kinet.dto.notificacion;

import co.edu.uniquindio.kinet.model.Notificacion;

import java.time.LocalDateTime;

/** leida es true cuando tiene fechaLectura. */
public record NotificacionDTO(Long id, String tipo, String canal, String titulo, String mensaje, String estadoEnvio,
                              boolean leida, LocalDateTime fechaCreacion, LocalDateTime fechaEnvio,
                              LocalDateTime fechaLectura) {
    public static NotificacionDTO de(Notificacion n) {
        return new NotificacionDTO(n.getId(), n.getTipo().name(), n.getCanal().name(), n.getTitulo(), n.getMensaje(),
                n.getEstadoEnvio().name(), n.getFechaLectura() != null, n.getFechaCreacion(), n.getFechaEnvio(),
                n.getFechaLectura());
    }
}
