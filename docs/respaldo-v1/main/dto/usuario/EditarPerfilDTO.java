package co.edu.uniquindio.kinet.dto.usuario;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record EditarPerfilDTO(
        @NotBlank(message = "Escribe tu nombre.")
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres.")
        String nombre,

        @NotBlank(message = "Elige un nickname.")
        @Size(min = 3, max = 30, message = "El nickname debe tener entre 3 y 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El nickname solo puede tener letras, números y guion bajo.")
        String nickname,

        @Size(max = 300, message = "La dirección de la imagen es demasiado larga.")
        @Pattern(regexp = "^(https?://.+)?$", message = "La imagen debe ser una dirección que empiece por http o https.")
        String avatarUrl,

        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento,

        @Size(max = 80, message = "La ciudad no puede tener más de 80 caracteres.")
        String ciudad,

        @Size(max = 50, message = "El deporte favorito no puede tener más de 50 caracteres.")
        String deporteFavorito
) {
}
