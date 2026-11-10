package co.edu.uniquindio.kinet.dto.autorizacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** El menor pide que se le envíe otra vez el correo a su representante (el enlace anterior vence). */
public record ReenvioAutorizacionDTO(
        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo
) {
}
