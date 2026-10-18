package co.edu.uniquindio.kinet.dto.evaluacion;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

/** Pregunta con sus opciones. Cada opción vota por un nivel distinto. */
public record GuardarPreguntaDTO(
        @NotBlank(message = "Escribe el enunciado.")
        @Size(max = 300, message = "El enunciado no puede tener más de 300 caracteres.")
        String enunciado,
        @NotNull(message = "Indica el orden.")
        @Min(value = 1, message = "El orden debe ser al menos 1.")
        @Max(value = 99, message = "El orden no puede ser mayor que 99.")
        Integer orden,
        @NotNull(message = "Agrega las opciones.")
        @Size(min = 2, message = "La pregunta necesita al menos 2 opciones.")
        List<@Valid OpcionNuevaDTO> opciones
) {
    public record OpcionNuevaDTO(
            @NotBlank(message = "Escribe el título de la opción.")
            @Size(max = 100, message = "El título no puede tener más de 100 caracteres.")
            String titulo,
            @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
            String descripcion,
            @NotBlank(message = "Indica el nivel al que vota la opción.")
            String nivelHabilidad,
            @NotNull(message = "Indica el peso de la opción.")
            @Min(value = 1, message = "El peso debe estar entre 1 y 10.")
            @Max(value = 10, message = "El peso debe estar entre 1 y 10.")
            Integer puntaje
    ) {
    }
}
