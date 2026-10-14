package co.edu.uniquindio.kinet.dto.entrenamiento;

import co.edu.uniquindio.kinet.model.enums.Intensidad;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Sesión que reporta el jugador. Los límites de duración y horario no se validan aquí sino en el
 * anti-trampa, para que un registro sospechoso quede guardado como RECHAZADO con su motivo.
 */
public record RegistrarEntrenamientoDTO(
        @NotBlank(message = "Elige el tipo de actividad.")
        String codigoActividad,

        @NotNull(message = "Indica cuántos minutos entrenaste.")
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.")
        @Max(value = 1440, message = "La duración no puede superar un día.")
        Integer duracionMinutos,

        @NotNull(message = "Elige la intensidad.")
        Intensidad intensidad,

        @NotNull(message = "Indica cuándo empezaste a entrenar.")
        LocalDateTime fechaInicio
) {
}
