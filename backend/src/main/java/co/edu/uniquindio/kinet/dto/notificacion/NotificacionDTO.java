package co.edu.uniquindio.kinet.dto.notificacion;

import java.time.LocalDateTime;

public record NotificacionDTO(Long id, String tipo, String mensaje, Boolean leida, LocalDateTime fecha) {
}
