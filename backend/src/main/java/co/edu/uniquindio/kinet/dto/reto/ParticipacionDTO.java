package co.edu.uniquindio.kinet.dto.reto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** progreso: lo aportado por el jugador. progresoClan: suma del clan (solo retos de CLAN). porcentaje: hacia la meta. */
public record ParticipacionDTO(
        Long id,
        Long idReto,
        String reto,
        String metrica,
        String ambito,
        LocalDate periodo,
        LocalDateTime fechaLimite,
        BigDecimal progreso,
        BigDecimal progresoClan,
        BigDecimal meta,
        Integer porcentaje,
        String estado,
        LocalDateTime fechaInscripcion,
        LocalDateTime fechaCompletado
) {
}
