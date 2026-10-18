package co.edu.uniquindio.kinet.dto.competencia;

import java.time.LocalDateTime;
import java.util.List;

/** fechaFin: fin de la ronda en curso (o de la competencia, si terminó). premioXp: lo que recibe el ganador. */
public record CompetenciaDTO(Long id, String tipo, String nombre, String estado, Long idCreador, String creador,
                             String actividad, Integer duracionRondaHoras, Integer rondaActual, Integer premioXp,
                             LocalDateTime fechaCreacion, LocalDateTime fechaInicio, LocalDateTime fechaFin,
                             List<ParticipanteCompetenciaDTO> participantes) {
}
