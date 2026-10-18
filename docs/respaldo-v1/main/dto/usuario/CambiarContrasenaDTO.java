package co.edu.uniquindio.kinet.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaDTO(
        @NotBlank(message = "Escribe tu contraseña actual.")
        String contrasenaActual,

        @NotBlank(message = "Escribe la nueva contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe incluir al menos una letra y un número.")
        String nuevaContrasena
) {
}
