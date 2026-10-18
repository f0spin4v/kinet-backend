package co.edu.uniquindio.kinet.dto.temporada;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record GuardarTemporadaDTO(
        @NotBlank(message = "Escribe el nombre de la temporada.")
        @Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
        String nombre,
        @NotNull(message = "Indica la fecha de inicio.")
        LocalDate fechaInicio,
        @NotNull(message = "Indica la fecha de fin.")
        LocalDate fechaFin,
        @NotNull(message = "Indica el porcentaje de puntos que se conserva.")
        @Min(value = 0, message = "El porcentaje debe estar entre 0 y 100.")
        @Max(value = 100, message = "El porcentaje debe estar entre 0 y 100.")
        Integer porcentajeConservado
) {
}
