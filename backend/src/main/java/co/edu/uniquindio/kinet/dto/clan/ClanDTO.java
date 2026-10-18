package co.edu.uniquindio.kinet.dto.clan;

import co.edu.uniquindio.kinet.model.Clan;
import java.time.LocalDateTime;

public record ClanDTO(Long id, String nombre, String descripcion, Integer capacidadMax, long miembros, String lider,
                      String estado, LocalDateTime fechaCreacion) {
    public static ClanDTO de(Clan clan, long miembros, String lider) {
        return new ClanDTO(clan.getId(), clan.getNombre(), clan.getDescripcion(), clan.getCapacidadMax(), miembros,
                lider, clan.getEstado().name(), clan.getFechaCreacion());
    }
}
