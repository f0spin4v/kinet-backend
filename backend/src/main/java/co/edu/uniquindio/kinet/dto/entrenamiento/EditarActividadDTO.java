package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** El administrador ajusta cuánto XP da cada minuto de una actividad. */
public record EditarActividadDTO(
        @NotNull(message = "Indica el XP por minuto.")
        @Min(value = 1, message = "El XP por minuto debe ser al menos 1.")
        @Max(value = 20, message = "El XP por minuto no puede ser mayor que 20.")
        Integer xpPorMinuto
) {
}
