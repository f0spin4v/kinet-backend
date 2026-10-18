package co.edu.uniquindio.kinet.dto.matchmaking;

import java.time.LocalDateTime;
import java.util.List;

public record EnfrentamientoDTO(Long id, String temporada, String tipo, String estado, LocalDateTime fechaInicio,
                                LocalDateTime fechaFin, List<ParticipanteDTO> participantes) {
}
