package co.edu.uniquindio.kinet.config;

import co.edu.uniquindio.kinet.services.AvisoRevisionService;
import co.edu.uniquindio.kinet.services.CompetenciaService;
import co.edu.uniquindio.kinet.services.EntrenamientoService;
import co.edu.uniquindio.kinet.services.MatchmakingService;
import co.edu.uniquindio.kinet.services.RetoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Trabajo periódico: cerrar cronómetros abandonados, rechazar las sesiones largas sin enlace de evidencia, vencer participaciones de retos, cerrar enfrentamientos
 * cuya ventana terminó, avanzar las competencias (empezarlas, cerrar rondas, vencer invitaciones) y avisar a los
 * entrenadores de las revisiones atrasadas más de 48 horas.
 * Las consultas del jugador también lo hacen, así que esto solo mantiene la base al día entre consultas.
 * Se apaga con kinet.tareas.habilitadas=false (pruebas).
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kinet.tareas.habilitadas", havingValue = "true", matchIfMissing = true)
public class TareasProgramadas {

    private final EntrenamientoService entrenamientoService;
    private final RetoService retoService;
    private final MatchmakingService matchmakingService;
    private final CompetenciaService competenciaService;
    private final AvisoRevisionService avisoRevisionService;

    @Scheduled(fixedDelayString = "${kinet.tareas.intervalo-ms:60000}", initialDelay = 30000)
    public void mantenimiento() {
        int abandonados = entrenamientoService.cerrarAbandonados() + entrenamientoService.rechazarSinEvidencia();
        int retos = retoService.vencerTodas();
        int enfrentamientos = matchmakingService.finalizarVencidos();
        int competencias = competenciaService.procesar();
        int avisos = avisoRevisionService.avisarAtrasadas();
        if (avisos > 0) {
            log.info("Tareas: {} entrenadores avisados de revisiones atrasadas", avisos);
        }
        if (abandonados + retos + enfrentamientos + competencias > 0) {
            log.info("Tareas: {} cronómetros cerrados, {} participaciones vencidas, {} enfrentamientos finalizados,"
                    + " {} competencias actualizadas", abandonados, retos, enfrentamientos, competencias);
        }
    }
}
