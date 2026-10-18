package co.edu.uniquindio.kinet.dto.temporada;

import co.edu.uniquindio.kinet.model.HistorialRango;

import java.time.LocalDateTime;

/** rangoAnterior es null en la inscripción. */
public record HistorialRangoDTO(String rangoAnterior, String rangoNuevo, Integer puntos, String causa, LocalDateTime fecha) {
    public static HistorialRangoDTO de(HistorialRango h) {
        return new HistorialRangoDTO(h.getRangoAnterior() == null ? null : h.getRangoAnterior().nombre(),
                h.getRangoNuevo().nombre(), h.getPuntos(), h.getCausa().name(), h.getFechaCambio());
    }
}
