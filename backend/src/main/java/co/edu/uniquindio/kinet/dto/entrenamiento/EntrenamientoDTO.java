package co.edu.uniquindio.kinet.dto.entrenamiento;

import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * minutosTranscurridos: solo mientras el cronómetro corre (EN_CURSO). xpOtorgado: 0 si no está validado.
 * evidencia: estado de la foto subida (SIN_REVISAR, ACEPTADA, RECHAZADA) o null si no tiene.
 */
public record EntrenamientoDTO(
        Long id,
        Long idTipoActividad,
        String actividad,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        Integer duracionMin,
        Integer minutosTranscurridos,
        String intensidad,
        BigDecimal distanciaKm,
        Integer caloriasKcal,
        Integer series,
        Integer repeticiones,
        BigDecimal pesoKg,
        String notas,
        String estado,
        String motivoRechazo,
        Integer xpOtorgado,
        String evidencia
) {
    public static EntrenamientoDTO de(Entrenamiento e, Integer xpOtorgado, String evidencia, LocalDateTime ahora) {
        Integer transcurridos = e.getEstadoValidacion() == EstadoValidacion.EN_CURSO
                ? (int) Duration.between(e.getFechaHoraInicio(), ahora).toMinutes() : null;
        return new EntrenamientoDTO(e.getId(), e.getTipoActividad().getId(), e.getTipoActividad().getNombre(),
                e.getFechaHoraInicio(), e.getFechaHoraFin(), e.getDuracionMin(), transcurridos,
                e.getIntensidad() == null ? null : e.getIntensidad().name(), e.getDistanciaKm(), e.getCaloriasKcal(),
                e.getSeries(), e.getRepeticiones(), e.getPesoKg(), e.getNotas(), e.getEstadoValidacion().name(), e.getMotivoRechazo(),
                xpOtorgado == null ? 0 : xpOtorgado, evidencia);
    }
}
