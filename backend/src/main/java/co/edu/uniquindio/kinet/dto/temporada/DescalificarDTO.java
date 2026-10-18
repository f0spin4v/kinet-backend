package co.edu.uniquindio.kinet.dto.temporada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DescalificarDTO(
        @NotBlank(message = "Escribe el motivo de la descalificación.")
        @Size(max = 200, message = "El motivo no puede tener más de 200 caracteres.")
        String motivo
) {
}
