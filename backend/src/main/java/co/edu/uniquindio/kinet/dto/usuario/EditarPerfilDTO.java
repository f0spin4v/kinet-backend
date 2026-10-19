package co.edu.uniquindio.kinet.dto.usuario;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EditarPerfilDTO(
        @NotBlank(message = "Escribe tus nombres.")
        @Size(max = 80, message = "Los nombres no pueden tener más de 80 caracteres.")
        String nombres,
        @NotBlank(message = "Escribe tus apellidos.")
        @Size(max = 80, message = "Los apellidos no pueden tener más de 80 caracteres.")
        String apellidos,
        @NotBlank(message = "Elige un gamertag.")
        @Size(min = 3, max = 30, message = "El gamertag debe tener entre 3 y 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El gamertag solo puede tener letras, números y guion bajo.")
        String gamertag,
        @Size(max = 300, message = "La dirección de la imagen es demasiado larga.")
        @Pattern(regexp = "^(https?://.+)?$", message = "La imagen debe ser una dirección que empiece por http o https.")
        String avatarUrl,
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento
) {
}
