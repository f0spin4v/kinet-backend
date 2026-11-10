package co.edu.uniquindio.kinet.dto.autorizacion;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * El administrador registra una autorización firmada por el representante legal (por ejemplo, en papel).
 * fechaNacimiento solo hace falta si el perfil del titular no la tiene.
 */
public record RegistrarAutorizacionDTO(
        @NotBlank(message = "Escribe el nombre del representante legal.")
        @Size(max = 160, message = "El nombre del representante no puede tener más de 160 caracteres.")
        String nombreRepresentante,
        @NotBlank(message = "Escribe el correo del representante legal.")
        @Email(message = "El correo del representante no tiene un formato válido.")
        @Size(max = 120, message = "El correo del representante no puede tener más de 120 caracteres.")
        String correoRepresentante,
        @NotBlank(message = "Elige el parentesco.")
        @Pattern(regexp = "^(MADRE|PADRE|TUTOR_LEGAL)$", message = "El parentesco debe ser MADRE, PADRE o TUTOR_LEGAL.")
        String parentesco,
        @NotBlank(message = "Describe el documento firmado y dónde quedó guardado.")
        @Size(min = 10, max = 300, message = "El soporte debe tener entre 10 y 300 caracteres.")
        String soporte,
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento
) {
}
