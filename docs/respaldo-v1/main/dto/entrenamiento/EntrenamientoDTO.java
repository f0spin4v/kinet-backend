package co.edu.uniquindio.kinet.dto.entrenamiento;

import java.time.LocalDateTime;

public record EntrenamientoDTO(
        Long id,
        String codigoActividad,
        String actividad,
        Integer duracionMinutos,
        String intensidad,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado,
        String motivoRechazo,
        Integer xpOtorgado
) {
}
