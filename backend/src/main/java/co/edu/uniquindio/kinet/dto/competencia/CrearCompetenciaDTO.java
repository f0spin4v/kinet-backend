package co.edu.uniquindio.kinet.dto.competencia;

import co.edu.uniquindio.kinet.model.enums.TipoCompetencia;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Competencia nueva. DUELO: 1 invitado y empieza cuando acepta (fechaInicio no se usa). GRUPAL: 2 a 9 invitados.
 * TORNEO: 3 a 15 invitados. Grupales y torneos empiezan en fechaInicio (dentro de los próximos 7 días).
 * idTipoActividad es opcional: si se indica, solo cuenta esa actividad.
 */
public record CrearCompetenciaDTO(
        @NotNull(message = "Elige el tipo: DUELO, GRUPAL o TORNEO.")
        TipoCompetencia tipo,
        @NotBlank(message = "Ponle un nombre a la competencia.")
        @Size(max = 80, message = "El nombre no puede tener más de 80 caracteres.")
        String nombre,
        @NotEmpty(message = "Invita al menos a un jugador.")
        List<@NotNull(message = "Falta un invitado.") Long> idsInvitados,
        Long idTipoActividad,
        @NotNull(message = "Indica cuántas horas dura cada ronda.")
        @Min(value = 1, message = "Una ronda dura al menos 1 hora.")
        @Max(value = 168, message = "Una ronda no puede durar más de 168 horas (una semana).")
        Integer duracionRondaHoras,
        LocalDateTime fechaInicio
) {
}
