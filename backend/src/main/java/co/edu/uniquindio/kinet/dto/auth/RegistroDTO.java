package co.edu.uniquindio.kinet.dto.auth;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegistroDTO(
        @NotBlank(message = "Escribe tus nombres.")
        @Size(max = 80, message = "Los nombres no pueden tener más de 80 caracteres.")
        String nombres,
        @NotBlank(message = "Escribe tus apellidos.")
        @Size(max = 80, message = "Los apellidos no pueden tener más de 80 caracteres.")
        String apellidos,
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 120, message = "El correo no puede tener más de 120 caracteres.")
        String correo,
        @NotBlank(message = "Escribe una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe incluir al menos una letra y un número.")
        String contrasena,
        @NotBlank(message = "Elige un gamertag.")
        @Size(min = 3, max = 30, message = "El gamertag debe tener entre 3 y 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El gamertag solo puede tener letras, números y guion bajo.")
        String gamertag,
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento,
        @AssertTrue(message = "Debes aceptar la política de tratamiento de datos.")
        boolean aceptaPolitica
) {
}
