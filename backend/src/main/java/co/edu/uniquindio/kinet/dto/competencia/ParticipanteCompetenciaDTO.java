package co.edu.uniquindio.kinet.dto.competencia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** puntaje incluye la ronda en curso mientras la competencia sigue (en vivo). */
public record ParticipanteCompetenciaDTO(Long idUsuario, String gamertag, String estadoInvitacion,
                                         LocalDateTime fechaRespuesta, BigDecimal puntaje, Integer eliminadoEnRonda,
                                         Integer posicion, String resultado) {
}
