package co.edu.uniquindio.kinet.dto.entrenamiento;

import co.edu.uniquindio.kinet.model.enums.Intensidad;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * "Finalizar entrenamiento". La hora de fin y la duración las calcula el servidor. Distancia y calorías son
 * opcionales e informativas: no dan XP ni cuentan para retos.
 */
public record FinalizarEntrenamientoDTO(
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
