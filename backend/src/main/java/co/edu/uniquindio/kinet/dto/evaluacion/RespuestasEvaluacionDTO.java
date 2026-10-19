package co.edu.uniquindio.kinet.dto.evaluacion;

import jakarta.validation.constraints.*;
import java.util.List;

/** Una opción por cada pregunta activa. */
public record RespuestasEvaluacionDTO(
        @NotEmpty(message = "Faltan las respuestas.")
        List<@NotNull(message = "Falta una respuesta.") Long> idsOpciones
) {
}
