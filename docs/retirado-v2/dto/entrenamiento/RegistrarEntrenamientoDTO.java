package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sesión reportada a mano. Los límites de duración y horario no se validan aquí sino en el anti-trampa,
 * para que un registro sospechoso quede guardado como RECHAZADO o PENDIENTE con su motivo.
 */
public record RegistrarEntrenamientoDTO(
        @NotNull(message = "Elige el tipo de actividad.")
        Long idTipoActividad,
        @NotNull(message = "Indica cuándo empezaste a entrenar.")
        LocalDateTime fechaHoraInicio,
        @NotNull(message = "Indica cuántos minutos entrenaste.")
        @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.")
        @Max(value = 1440, message = "La duración no puede superar un día.")
        Integer duracionMin,
        @NotNull(message = "Elige la intensidad.")
        Intensidad intensidad,
        @DecimalMin(value = "0", message = "La distancia no puede ser negativa.")
        @Digits(integer = 5, fraction = 2, message = "La distancia admite como máximo 2 decimales.")
        BigDecimal distanciaKm,
        @Min(value = 0, message = "Las calorías no pueden ser negativas.")
        @Max(value = 20000, message = "Las calorías no son válidas.")
        Integer caloriasKcal,
        @Size(max = 500, message = "Las notas no pueden tener más de 500 caracteres.")
        String notas
) {
}
