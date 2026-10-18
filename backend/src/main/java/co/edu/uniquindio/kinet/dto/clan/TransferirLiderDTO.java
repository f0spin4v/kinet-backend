package co.edu.uniquindio.kinet.dto.clan;

import jakarta.validation.constraints.*;

public record TransferirLiderDTO(@NotNull(message = "Indica quién será el nuevo líder.") Long idUsuario) {
}
