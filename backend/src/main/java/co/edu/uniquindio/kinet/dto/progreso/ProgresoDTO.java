package co.edu.uniquindio.kinet.dto.progreso;


/** Datos del dashboard (UI-003). xpSiguienteNivel es null en el nivel máximo; rango y puntos, null fuera de temporada. */
public record ProgresoDTO(
        String gamertag,
        int xpTotal,
        int nivel,
        int xpInicioNivel,
        Integer xpSiguienteNivel,
        int porcentajeNivel,
        int rachaActual,
        int rachaMaxima,
        long entrenamientosSemana,
        long minutosSemana,
        long retosActivos,
        long retosCompletados,
        long logros,
        long notificacionesSinLeer,
        String nivelHabilidad,
        String temporada,
        String rango,
        Integer puntosTemporada
) {
}
