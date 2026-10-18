package co.edu.uniquindio.kinet.dto.reto;

import java.time.LocalDateTime;

public record ParticipacionDTO(
        Long id,
        Long idReto,
        String tituloReto,
        Integer progreso,
        Integer meta,
        Integer porcentaje,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaLimite,
        LocalDateTime fechaCompletado
) {
}
