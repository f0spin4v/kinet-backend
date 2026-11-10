package co.edu.uniquindio.kinet.dto.autorizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El representante legal retiró la autorización (por ejemplo, por soporte): queda el motivo en la bitácora. */
public record RevocarAutorizacionDTO(
        @NotBlank(message = "Escribe el motivo de la revocación.")
        @Size(min = 5, max = 150, message = "El motivo debe tener entre 5 y 150 caracteres.")
        String motivo
) {
}
