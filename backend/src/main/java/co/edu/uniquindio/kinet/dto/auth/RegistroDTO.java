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
        // Ley 1581 de 2012: desde 14 años; de 14 a 17 se pide el representante legal (ver UsuarioService.EDAD_MINIMA)
        @NotNull(message = "Escribe tu fecha de nacimiento.")
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento,
        @AssertTrue(message = "Debes aceptar la política de tratamiento de datos.")
        boolean aceptaPolitica,
        // Solo para titulares de 14 a 17 años (RN-33): quién autoriza el tratamiento de sus datos
        @Size(max = 160, message = "El nombre del representante no puede tener más de 160 caracteres.")
        String nombreRepresentante,
        @Email(message = "El correo del representante no tiene un formato válido.")
        @Size(max = 120, message = "El correo del representante no puede tener más de 120 caracteres.")
        String correoRepresentante,
        @Pattern(regexp = "^(MADRE|PADRE|TUTOR_LEGAL)$", message = "El parentesco debe ser MADRE, PADRE o TUTOR_LEGAL.")
        String parentesco
) {
}
