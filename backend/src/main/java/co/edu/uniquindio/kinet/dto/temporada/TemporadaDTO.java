package co.edu.uniquindio.kinet.dto.temporada;

import co.edu.uniquindio.kinet.model.Temporada;
import java.time.LocalDate;

public record TemporadaDTO(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                           Integer porcentajeConservado, String estado) {
    public static TemporadaDTO de(Temporada t) {
        return new TemporadaDTO(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(),
                t.getPorcentajeConservado(), t.getEstado().name());
    }
}
