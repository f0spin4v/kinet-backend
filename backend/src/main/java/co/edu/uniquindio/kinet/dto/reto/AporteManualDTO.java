package co.edu.uniquindio.kinet.dto.reto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Aporte manual a un reto de REPETICIONES (por ejemplo una serie de flexiones). */
public record AporteManualDTO(
        @NotNull(message = "Indica la cantidad.")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0.")
        @DecimalMax(value = "10000", message = "La cantidad es demasiado alta.")
        BigDecimal cantidad,
        @Size(max = 200, message = "La nota no puede tener más de 200 caracteres.")
        String nota
) {
}
