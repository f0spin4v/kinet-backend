package co.edu.uniquindio.kinet.dto.reto;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.AmbitoReto;
import co.edu.uniquindio.kinet.model.enums.MetricaReto;
import co.edu.uniquindio.kinet.model.enums.Periodicidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Reto nuevo o editado (solo en BORRADOR). Actividad, nivel y temporada son opcionales. */
public record GuardarRetoDTO(
        @NotBlank(message = "Escribe el nombre del reto.")
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres.")
        String nombre,
        @NotBlank(message = "Escribe la descripción del reto.")
        @Size(max = 500, message = "La descripción no puede tener más de 500 caracteres.")
        String descripcion,
        @Size(max = 300, message = "Las condiciones no pueden tener más de 300 caracteres.")
        String condiciones,
        @Size(max = 300, message = "La dirección de la imagen es demasiado larga.")
        String imagenUrl,
        @NotNull(message = "Elige la métrica: MINUTOS, SESIONES o REPETICIONES.")
        MetricaReto metrica,
        @NotNull(message = "Indica la meta.")
        @DecimalMin(value = "0.01", message = "La meta debe ser mayor que 0.")
        @DecimalMax(value = "100000", message = "La meta es demasiado alta.")
        BigDecimal valorMeta,
        @NotNull(message = "Indica el XP de recompensa.")
        @Min(value = 1, message = "La recompensa debe ser al menos 1 XP.")
        @Max(value = 5000, message = "La recompensa es demasiado alta.")
        Integer xpRecompensa,
        @NotNull(message = "Elige la periodicidad.")
        Periodicidad periodicidad,
        @NotNull(message = "Elige el ámbito.")
        AmbitoReto ambito,
        Long idTipoActividad,
        Long idNivelHabilidad,
        Long idTemporada,
        @NotNull(message = "Indica desde cuándo está disponible.")
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {
}
