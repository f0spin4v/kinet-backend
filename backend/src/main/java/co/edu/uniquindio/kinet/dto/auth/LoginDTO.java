package co.edu.uniquindio.kinet.dto.auth;

import jakarta.validation.constraints.*;

public record LoginDTO(
        @NotBlank(message = "Escribe tu correo.") String correo,
        @NotBlank(message = "Escribe tu contraseña.") String contrasena
) {
}
