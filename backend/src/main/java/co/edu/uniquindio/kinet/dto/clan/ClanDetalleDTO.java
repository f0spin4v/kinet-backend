package co.edu.uniquindio.kinet.dto.clan;

import java.util.List;

public record ClanDetalleDTO(ClanDTO clan, List<MiembroDTO> miembros) {
}
