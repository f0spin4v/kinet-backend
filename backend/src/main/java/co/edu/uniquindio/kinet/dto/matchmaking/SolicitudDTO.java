package co.edu.uniquindio.kinet.dto.matchmaking;

import java.time.LocalDateTime;

/** Resultado de pedir un enfrentamiento: queda EN_COLA o EMPAREJADA con su enfrentamiento. */
public record SolicitudDTO(Long id, String estado, LocalDateTime fechaSolicitud, EnfrentamientoDTO enfrentamiento,
                           String mensaje) {
}
