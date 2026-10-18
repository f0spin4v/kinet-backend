package co.edu.uniquindio.kinet.dto.clan;

import java.time.LocalDateTime;

public record MiembroDTO(Long idUsuario, String gamertag, String rolClan, LocalDateTime fechaIngreso) {
}
