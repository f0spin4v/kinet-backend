package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.services.AntiTrampaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.kinet.services.ReglasJuego.*;

/**
 * Primera barrera de integridad mientras no haya integración con wearables (ADR-05 queda como evolución).
 * Revisa duración, fecha, cruces de horario y topes diarios.
 */
@Service
@RequiredArgsConstructor
public class AntiTrampaServiceImpl implements AntiTrampaService {

    private final EntrenamientoRepository entrenamientoRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> validar(Usuario usuario, LocalDateTime inicio, int duracionMinutos) {
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
        LocalDateTime ahora = LocalDateTime.now();

        if (duracionMinutos < DURACION_MINIMA) {
            return motivo("La sesión debe durar al menos " + DURACION_MINIMA + " minutos para contar.");
        }
        if (duracionMinutos > DURACION_MAXIMA) {
            return motivo("Una sola sesión no puede durar más de " + DURACION_MAXIMA + " minutos.");
        }
        if (inicio.isAfter(ahora.plusMinutes(MINUTOS_TOLERANCIA_FUTURO))) {
            return motivo("La fecha de inicio no puede estar en el futuro.");
        }
        if (fin.isAfter(ahora.plusMinutes(MINUTOS_TOLERANCIA_FUTURO))) {
            return motivo("Según la hora de inicio y la duración, la sesión todavía no ha terminado.");
        }
        if (inicio.isBefore(ahora.minusHours(HORAS_MAXIMAS_ATRAS))) {
            return motivo("Solo puedes registrar entrenamientos de las últimas " + HORAS_MAXIMAS_ATRAS + " horas.");
        }
        if (entrenamientoRepository.existsByUsuarioIdAndEstadoAndFechaInicioLessThanAndFechaFinGreaterThan(
                usuario.getId(), EstadoEntrenamiento.VALIDADO, fin, inicio)) {
            return motivo("Se cruza con otro entrenamiento que ya registraste en ese horario.");
        }

        LocalDate dia = inicio.toLocalDate();
        List<Entrenamiento> delDia = entrenamientoRepository.findByUsuarioIdAndEstadoAndFechaInicioBetween(
                usuario.getId(), EstadoEntrenamiento.VALIDADO, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay().minusNanos(1));
        if (delDia.size() >= MAX_SESIONES_DIA) {
            return motivo("Ya registraste " + MAX_SESIONES_DIA + " entrenamientos ese día, el máximo permitido.");
        }
        int minutosDelDia = delDia.stream().mapToInt(Entrenamiento::getDuracionMinutos).sum();
        if (minutosDelDia + duracionMinutos > MAX_MINUTOS_DIA) {
            return motivo("Superarías el máximo de " + MAX_MINUTOS_DIA + " minutos de entrenamiento en un día.");
        }
        return Optional.empty();
    }

    private static Optional<String> motivo(String texto) {
        return Optional.of(texto);
    }
}
