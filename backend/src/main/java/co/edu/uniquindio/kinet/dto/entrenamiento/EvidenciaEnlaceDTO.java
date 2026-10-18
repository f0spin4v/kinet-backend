package co.edu.uniquindio.kinet.dto.entrenamiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Enlace a la actividad en una app deportiva reconocida (Strava, Garmin Connect...). */
public record EvidenciaEnlaceDTO(
        @NotBlank(message = "Pega el enlace de la actividad.")
        @Size(max = 500, message = "El enlace no puede tener más de 500 caracteres.")
        String url
) {
}
