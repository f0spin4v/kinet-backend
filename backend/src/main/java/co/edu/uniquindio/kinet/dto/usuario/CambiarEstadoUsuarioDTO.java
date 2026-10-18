package co.edu.uniquindio.kinet.dto.usuario;

import jakarta.validation.constraints.*;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;

public record CambiarEstadoUsuarioDTO(
        @NotNull(message = "Indica el nuevo estado.") EstadoUsuario estado
) {
}
