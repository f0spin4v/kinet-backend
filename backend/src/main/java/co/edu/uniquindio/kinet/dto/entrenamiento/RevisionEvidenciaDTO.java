package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Decisión del entrenador sobre una evidencia. Para rechazarla, la nota es obligatoria. */
public record RevisionEvidenciaDTO(
        @NotNull(message = "Indica si aceptas la evidencia.")
        Boolean aceptar,
        @Size(max = 200, message = "La nota no puede tener más de 200 caracteres.")
        String nota
) {
}
