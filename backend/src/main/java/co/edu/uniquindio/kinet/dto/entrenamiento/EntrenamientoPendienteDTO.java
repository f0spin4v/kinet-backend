package co.edu.uniquindio.kinet.dto.entrenamiento;


/** Entrenamiento que espera revisión, con el jugador que lo reportó. */
public record EntrenamientoPendienteDTO(Long idUsuario, String gamertag, EntrenamientoDTO entrenamiento) {
}
