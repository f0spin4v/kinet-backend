package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.*;

/** Decisión del entrenador. Para rechazar, el motivo es obligatorio. */
public record RevisionEntrenamientoDTO(
        @NotNull(message = "Indica si apruebas el entrenamiento.")
        Boolean aprobar,
        @Size(max = 200, message = "El motivo no puede tener más de 200 caracteres.")
        String motivo
) {
}
