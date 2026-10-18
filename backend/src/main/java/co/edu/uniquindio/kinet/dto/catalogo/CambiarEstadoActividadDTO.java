package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.enums.EstadoActividad;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoActividadDTO(@NotNull(message = "Indica el estado: ACTIVA o INACTIVA.") EstadoActividad estado) {
}
