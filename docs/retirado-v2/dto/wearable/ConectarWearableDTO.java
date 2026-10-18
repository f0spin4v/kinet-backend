package co.edu.uniquindio.kinet.dto.wearable;

import jakarta.validation.constraints.*;

public record ConectarWearableDTO(
        @NotNull(message = "Elige el proveedor.")
        Long idProveedor,
        @NotBlank(message = "Falta la cuenta del proveedor.")
        @Size(max = 100, message = "La cuenta no puede tener más de 100 caracteres.")
        String idCuentaExterna,
        @NotBlank(message = "Falta el token de acceso del proveedor.")
        @Size(max = 300, message = "El token es demasiado largo.")
        String tokenAcceso
) {
}
