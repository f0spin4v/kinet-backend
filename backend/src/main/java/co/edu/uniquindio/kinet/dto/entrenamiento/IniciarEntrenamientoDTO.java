package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.NotNull;

/** "Iniciar entrenamiento": solo la actividad. La hora de inicio la pone el servidor. */
public record IniciarEntrenamientoDTO(
        @NotNull(message = "Elige el tipo de actividad.")
        Long idTipoActividad
) {
}
