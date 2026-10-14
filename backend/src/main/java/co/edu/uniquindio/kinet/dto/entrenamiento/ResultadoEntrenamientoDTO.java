package co.edu.uniquindio.kinet.dto.entrenamiento;

import java.util.List;

/**
 * Respuesta al registrar un entrenamiento: el registro y su efecto en el progreso del jugador.
 * Si el anti-trampa lo rechaza, estado = RECHAZADO, motivoRechazo explica la causa y no se suma XP.
 */
public record ResultadoEntrenamientoDTO(
        EntrenamientoDTO entrenamiento,
        int xpGanado,
        int xpTotal,
        int nivel,
        boolean subioNivel,
        int rachaActual,
        List<String> retosCompletados
) {
}
