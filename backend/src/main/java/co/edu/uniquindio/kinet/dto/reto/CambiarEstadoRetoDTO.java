package co.edu.uniquindio.kinet.dto.reto;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.EstadoReto;

public record CambiarEstadoRetoDTO(@NotNull(message = "Indica el estado.") EstadoReto estado) {
}
