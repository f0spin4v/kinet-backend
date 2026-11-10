package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.EvidenciaEntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.NotificacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * KAN-85: si hay sesiones o evidencias esperando revisión hace más de 48 horas, avisa a cada entrenador activo
 * (bandeja y correo). Como mucho un aviso por entrenador cada 24 horas, para no llenarle la bandeja.
 */
@Service
@RequiredArgsConstructor
public class AvisoRevisionService {

    public static final int HORAS_ATRASO = 48;
    public static final int HORAS_ENTRE_AVISOS = 24;
    public static final String TITULO = "Revisiones atrasadas";

    private final EntrenamientoRepository entrenamientoRepository;
    private final EvidenciaEntrenamientoRepository evidenciaRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    /** Devuelve a cuántos entrenadores avisó. */
    @Transactional
    public int avisarAtrasadas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.minusHours(HORAS_ATRASO);
        long sesiones = entrenamientoRepository.countByEstadoValidacionAndFechaHoraFinBefore(EstadoValidacion.PENDIENTE, limite);
        long evidencias = evidenciaRepository.countByEstadoRevisionAndFechaSubidaBefore(EstadoEvidencia.SIN_REVISAR, limite);
        if (sesiones + evidencias == 0) {
            return 0;
        }
        String mensaje = "Hay " + cantidad(sesiones, "sesión", "sesiones") + " y " + cantidad(evidencias, "evidencia", "evidencias")
                + " esperando revisión hace más de " + HORAS_ATRASO + " horas. Los jugadores no reciben su XP hasta que "
                + "las revises en Moderación.";
        int avisados = 0;
        for (Usuario entrenador : usuarioRepository.conRolYEstado(Rol.ENTRENADOR, EstadoUsuario.ACTIVO)) {
            if (notificacionRepository.existsByUsuarioIdAndTituloAndFechaCreacionAfter(entrenador.getId(), TITULO,
                    ahora.minusHours(HORAS_ENTRE_AVISOS))) {
                continue;
            }
            notificacionService.notificar(entrenador, TipoNotificacion.VALIDACION, CanalNotificacion.EMAIL, TITULO, mensaje);
            avisados++;
        }
        return avisados;
    }

    private static String cantidad(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
