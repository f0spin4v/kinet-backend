package co.edu.uniquindio.kinet.dto.matchmaking;

import java.math.BigDecimal;

/** puntaje: XP de entrenamientos dentro de la ventana (en vivo mientras el enfrentamiento sigue en curso). */
public record ParticipanteDTO(Long idUsuario, String gamertag, BigDecimal puntaje, String resultado) {
}
