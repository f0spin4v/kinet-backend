package co.edu.uniquindio.kinet.dto.wearable;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

public record SincronizacionDTO(
        @NotEmpty(message = "No hay actividades para sincronizar.")
        @Size(max = 50, message = "Se pueden sincronizar como máximo 50 actividades a la vez.")
        List<@Valid ActividadExternaDTO> actividades
) {
}
