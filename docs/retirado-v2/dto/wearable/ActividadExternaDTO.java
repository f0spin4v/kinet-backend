package co.edu.uniquindio.kinet.dto.wearable;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Actividad tal como la entrega el proveedor. idActividadExterna evita importarla dos veces. */
public record ActividadExternaDTO(
        @NotBlank(message = "Falta el identificador de la actividad en el proveedor.")
        @Size(max = 100, message = "El identificador es demasiado largo.")
        String idActividadExterna,
        @NotNull(message = "Falta el tipo de actividad.")
        Long idTipoActividad,
        @NotNull(message = "Falta la hora de inicio.")
        LocalDateTime fechaHoraInicio,
        @NotNull(message = "Falta la duración.")
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.")
        @Max(value = 1440, message = "La duración no puede superar un día.")
        Integer duracionMin,
        @NotNull(message = "Falta la intensidad.")
        Intensidad intensidad,
        @DecimalMin(value = "0", message = "La distancia no puede ser negativa.")
        BigDecimal distanciaKm,
        @Min(value = 0, message = "Las calorías no pueden ser negativas.")
        Integer caloriasKcal
) {
}
