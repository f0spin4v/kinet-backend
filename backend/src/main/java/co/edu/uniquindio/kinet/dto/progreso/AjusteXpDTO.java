package co.edu.uniquindio.kinet.dto.progreso;

import jakarta.validation.constraints.*;

/** Corrección manual de XP. Puede ser negativa, pero el saldo del jugador nunca queda por debajo de 0. */
public record AjusteXpDTO(
        @NotNull(message = "Indica el jugador.")
        Long idUsuario,
        @NotNull(message = "Indica la cantidad.")
        @Min(value = -10000, message = "La cantidad es demasiado baja.")
        @Max(value = 10000, message = "La cantidad es demasiado alta.")
        Integer cantidad,
        @NotBlank(message = "El motivo del ajuste es obligatorio.")
        @Size(max = 200, message = "El motivo no puede tener más de 200 caracteres.")
        String motivo
) {
}
