package co.edu.uniquindio.kinet.dto.evaluacion;

import jakarta.validation.constraints.*;

public record CambiarEstadoPreguntaDTO(@NotNull(message = "Indica si la pregunta queda activa.") Boolean activa) {
}
