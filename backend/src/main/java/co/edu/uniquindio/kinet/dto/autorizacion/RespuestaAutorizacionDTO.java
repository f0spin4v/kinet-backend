package co.edu.uniquindio.kinet.dto.autorizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Respuesta del representante legal: autoriza o no el tratamiento de los datos del menor. */
public record RespuestaAutorizacionDTO(
        @NotBlank(message = "Falta el enlace de autorización.")
        String token,
        @NotNull(message = "Indica si autorizas o no.")
        Boolean autoriza
) {
}
