package co.edu.uniquindio.kinet.dto.auth;

import jakarta.validation.constraints.*;

public record RecuperarContrasenaDTO(
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo
) {
}
