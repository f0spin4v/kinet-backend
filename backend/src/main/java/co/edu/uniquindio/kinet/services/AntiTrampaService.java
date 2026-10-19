package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.kinet.services.ReglasJuego.*;

/**
 * Revisa una sesión al finalizar el cronómetro. Como el inicio y el fin los pone el servidor, ya no hace falta
 * revisar fechas futuras, sesiones viejas ni cruces de horario: quedan la duración y los topes diarios.
 * Lo claramente inválido queda RECHAZADO; lo posible pero sospechoso (sesiones muy largas) queda PENDIENTE.
 */
@Service
@RequiredArgsConstructor
public class AntiTrampaService {

    private static final List<EstadoValidacion> QUE_CUENTAN = List.of(EstadoValidacion.VALIDADO,
            EstadoValidacion.PENDIENTE);

    private final EntrenamientoRepository entrenamientoRepository;

    public record Veredicto(EstadoValidacion estado, String motivo) {
        static Veredicto validado() {
            return new Veredicto(EstadoValidacion.VALIDADO, null);
        }

        static Veredicto rechazado(String motivo) {
            return new Veredicto(EstadoValidacion.RECHAZADO, motivo);
        }
    }

    /** @param duracionMinima la de la regla de puntuación de la actividad e intensidad. */
    @Transactional(readOnly = true)
    public Veredicto validar(Usuario usuario, LocalDateTime inicio, int duracion, int duracionMinima) {
        if (duracion < duracionMinima) {
            return Veredicto.rechazado("La sesión debe durar al menos " + duracionMinima + " minutos para contar.");
        }
        if (duracion > DURACION_MAXIMA) {
            return Veredicto.rechazado("Una sola sesión no puede durar más de " + DURACION_MAXIMA
                    + " minutos. ¿Olvidaste finalizar el cronómetro?");
        }
        LocalDate dia = inicio.toLocalDate();
        EntrenamientoRepository.TotalesDia delDia = entrenamientoRepository.totalesDelDia(usuario.getId(), QUE_CUENTAN,
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        if (delDia.getSesiones() >= MAX_SESIONES_DIA) {
            return Veredicto.rechazado("Ya tienes " + MAX_SESIONES_DIA + " entrenamientos ese día, el máximo permitido.");
        }
        if (delDia.getMinutos() + duracion > MAX_MINUTOS_DIA) {
            return Veredicto.rechazado("Superarías el máximo de " + MAX_MINUTOS_DIA + " minutos de entrenamiento en un día.");
        }
        if (duracion > UMBRAL_REVISION) {
            return new Veredicto(EstadoValidacion.PENDIENTE, "Sesión de más de " + UMBRAL_REVISION
                    + " minutos: queda pendiente de revisión por un entrenador.");
        }
        return Veredicto.validado();
    }
}
