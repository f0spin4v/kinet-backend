package co.edu.uniquindio.kinet.dto.logro;

import co.edu.uniquindio.kinet.model.Logro;
import java.time.LocalDateTime;

public record LogroDTO(Long id, String nombre, String descripcion, String criterio, Integer xpBonus, String iconoUrl,
                       boolean obtenido, LocalDateTime fechaObtenido) {
    public static LogroDTO de(Logro l, LocalDateTime fechaObtenido) {
        return new LogroDTO(l.getId(), l.getNombre(), l.getDescripcion(), l.getCriterio(), l.getXpBonus(),
                l.getIconoUrl(), fechaObtenido != null, fechaObtenido);
    }
}
