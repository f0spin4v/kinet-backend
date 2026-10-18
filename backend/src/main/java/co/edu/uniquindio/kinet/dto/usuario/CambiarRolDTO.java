package co.edu.uniquindio.kinet.dto.usuario;

import jakarta.validation.constraints.*;

public record CambiarRolDTO(
        @NotBlank(message = "Indica el rol.")
        @Pattern(regexp = "JUGADOR|ADMINISTRADOR|ENTRENADOR", message = "El rol no existe.")
        String rol
) {
}
