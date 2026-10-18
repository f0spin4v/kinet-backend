package co.edu.uniquindio.kinet.dto.catalogo;

import jakarta.validation.constraints.*;

public record EditarNivelProgresionDTO(
        @NotNull(message = "Indica el XP requerido.")
        @Min(value = 0, message = "El XP requerido no puede ser negativo.")
        Integer xpRequerido
) {
}
