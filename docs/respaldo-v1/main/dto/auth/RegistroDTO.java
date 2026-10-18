package co.edu.uniquindio.kinet.dto.auth;

import jakarta.validation.constraints.*;

public record RegistroDTO(
        @NotBlank(message = "Escribe tu nombre.")
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres.")
        String nombre,

        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 150, message = "El correo no puede tener más de 150 caracteres.")
        String correo,

        @NotBlank(message = "Escribe una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe incluir al menos una letra y un número.")
        String contrasena,

        @NotBlank(message = "Elige un nickname.")
        @Size(min = 3, max = 30, message = "El nickname debe tener entre 3 y 30 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El nickname solo puede tener letras, números y guion bajo.")
        String nickname,

        @AssertTrue(message = "Debes aceptar la política de tratamiento de datos.")
        boolean aceptaDatos
) {
}
