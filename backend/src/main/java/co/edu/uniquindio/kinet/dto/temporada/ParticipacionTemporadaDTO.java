package co.edu.uniquindio.kinet.dto.temporada;

import co.edu.uniquindio.kinet.dto.catalogo.RangoDTO;
import java.time.LocalDateTime;

/** posicion: lugar actual en el ranking (o el final, si la temporada cerró). */
public record ParticipacionTemporadaDTO(
        TemporadaDTO temporada,
        Integer puntosIniciales,
        Integer puntosActuales,
        RangoDTO rangoInicial,
        RangoDTO rangoActual,
        Integer posicion,
        Integer puntosFinales,
        String estado,
        LocalDateTime fechaInscripcion
) {
}
