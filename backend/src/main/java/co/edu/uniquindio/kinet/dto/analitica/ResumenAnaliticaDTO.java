package co.edu.uniquindio.kinet.dto.analitica;

import java.util.Map;

/** Indicadores agregados para el administrador. Las ventanas "30 días" cuentan desde hoy hacia atrás. */
public record ResumenAnaliticaDTO(
        Map<String, Long> usuariosPorEstado,
        Map<String, Long> usuariosPorRol,
        Map<String, Long> jugadoresPorNivelHabilidad,
        Map<String, Long> entrenamientosPorEstado30Dias,
        Map<String, Long> minutosPorActividad30Dias,
        Map<String, Long> xpPorOrigen30Dias,
        Map<String, Long> retosCompletados30Dias,
        Map<String, Long> jugadoresPorLigaTemporadaActiva,
        Map<String, Long> competenciasPorTipo,
        Map<String, Long> notificacionesPorEstadoEnvio
) {
}
