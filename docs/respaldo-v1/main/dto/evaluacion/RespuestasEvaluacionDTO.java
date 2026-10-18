package co.edu.uniquindio.kinet.dto.evaluacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Respuestas en orden, una por pregunta. Cada una es "A" o "B". */
public record RespuestasEvaluacionDTO(
        @NotNull(message = "Faltan las respuestas.")
        @Size(min = 5, max = 5, message = "Debes responder las 5 preguntas.")
        List<@NotNull(message = "Falta una respuesta.")
        @Pattern(regexp = "[AB]", message = "Cada respuesta debe ser A o B.") String> respuestas
) {
}
