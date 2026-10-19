package co.edu.uniquindio.kinet.dto.entrenamiento;

import java.util.List;

/**
 * Respuesta al registrar o aprobar un entrenamiento. xpGanado suma el entrenamiento, los retos completados
 * y los logros desbloqueados. Si quedó RECHAZADO o PENDIENTE, no suma XP.
 */
public record ResultadoEntrenamientoDTO(
        EntrenamientoDTO entrenamiento,
        int xpGanado,
        int xpTotal,
        int nivel,
        boolean subioNivel,
        int rachaActual,
        List<String> retosCompletados,
        List<String> logrosObtenidos
) {
}
