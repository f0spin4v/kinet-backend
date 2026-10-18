package co.edu.uniquindio.kinet.dto.progreso;

import co.edu.uniquindio.kinet.model.MovimientoXp;
import java.time.LocalDateTime;

/** idReferencia: el entrenamiento, la participación en reto, el logro, la evidencia o la competencia del movimiento. */
public record MovimientoXpDTO(Long id, String origen, Integer cantidad, String motivo, Long idReferencia,
                              LocalDateTime fecha) {
    public static MovimientoXpDTO de(MovimientoXp m) {
        Long referencia = m.getEntrenamiento() != null ? m.getEntrenamiento().getId()
                : m.getParticipacionReto() != null ? m.getParticipacionReto().getId()
                : m.getLogro() != null ? m.getLogro().getId()
                : m.getEvidencia() != null ? m.getEvidencia().getId()
                : m.getCompetencia() != null ? m.getCompetencia().getId() : null;
        return new MovimientoXpDTO(m.getId(), m.getOrigen().name(), m.getCantidad(), m.getMotivo(), referencia,
                m.getFechaMovimiento());
    }
}
