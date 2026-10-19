package co.edu.uniquindio.kinet.dto.reto;

import co.edu.uniquindio.kinet.model.Reto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Reto del catálogo. participacion trae el avance del jugador en el periodo actual, o null. */
public record RetoDTO(
        Long id,
        String nombre,
        String descripcion,
        String condiciones,
        String imagenUrl,
        String metrica,
        BigDecimal valorMeta,
        Integer xpRecompensa,
        String periodicidad,
        String ambito,
        String actividad,
        String nivelHabilidad,
        String temporada,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado,
        ParticipacionDTO participacion
) {
    public static RetoDTO de(Reto r, ParticipacionDTO participacion) {
        return new RetoDTO(r.getId(), r.getNombre(), r.getDescripcion(), r.getCondiciones(), r.getImagenUrl(),
                r.getMetrica().name(), r.getValorMeta(), r.getXpRecompensa(), r.getPeriodicidad().name(),
                r.getAmbito().name(),
                r.getTipoActividad() == null ? null : r.getTipoActividad().getNombre(),
                r.getNivelHabilidad() == null ? null : r.getNivelHabilidad().getNombre(),
                r.getTemporada() == null ? null : r.getTemporada().getNombre(),
                r.getFechaInicio(), r.getFechaFin(), r.getEstado().name(), participacion);
    }
}
