package co.edu.uniquindio.kinet.dto.progreso;

/** Datos del dashboard (UI-003). */
public record ProgresoDTO(
        int xpTotal,
        int nivel,
        int xpInicioNivel,
        int xpSiguienteNivel,
        int porcentajeNivel,
        int rachaActual,
        int rachaMaxima,
        int entrenamientosSemana,
        int minutosSemana,
        long retosActivos,
        long retosCompletados,
        long notificacionesSinLeer,
        String nivelInicial
) {
}
