package co.edu.uniquindio.kinet.dto.wearable;

import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import java.util.List;

/** duplicadas: actividades que ya se habían importado antes y se ignoraron. */
public record ResultadoSincronizacionDTO(
        int importadas,
        int duplicadas,
        int validadas,
        int pendientes,
        int rechazadas,
        int xpGanado,
        List<EntrenamientoDTO> entrenamientos
) {
}
