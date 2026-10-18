package co.edu.uniquindio.kinet.dto.catalogo;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Nueva regla. Si ya hay una vigente para esa actividad e intensidad, se cierra el día anterior a vigenteDesde. */
public record CrearReglaDTO(
        @NotNull(message = "Elige la actividad.")
        Long idTipoActividad,
        @NotNull(message = "Elige la intensidad.")
        Intensidad intensidad,
        @NotNull(message = "Indica el XP por minuto.")
        @DecimalMin(value = "0.01", message = "El XP por minuto debe ser mayor que 0.")
        @DecimalMax(value = "100", message = "El XP por minuto no puede ser mayor que 100.")
        @Digits(integer = 4, fraction = 2, message = "Usa como máximo 2 decimales.")
        BigDecimal xpPorMinuto,
        @NotNull(message = "Indica la duración mínima.")
        @Min(value = 1, message = "La duración mínima debe ser al menos 1 minuto.")
        @Max(value = 240, message = "La duración mínima no puede pasar de 240 minutos.")
        Integer duracionMinimaMin,
        @NotNull(message = "Indica desde cuándo aplica.")
        LocalDate vigenteDesde
) {
}
