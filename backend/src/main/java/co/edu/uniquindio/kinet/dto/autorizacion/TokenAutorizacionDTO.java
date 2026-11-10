package co.edu.uniquindio.kinet.dto.autorizacion;

import jakarta.validation.constraints.NotBlank;

/** El token del enlace que recibió el representante. Va en el cuerpo para que no quede en los registros de acceso. */
public record TokenAutorizacionDTO(
        @NotBlank(message = "Falta el enlace de autorización.")
        String token
) {
}
