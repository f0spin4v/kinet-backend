package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ProgresoJugador;
import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProgresoJugadorRepository extends JpaRepository<ProgresoJugador, Long> {
    Optional<ProgresoJugador> findByUsuarioId(Long idUsuario);

    /** Datos del dashboard en una sola consulta. Los campos del progreso vienen en null si el jugador aún no tiene. */
    interface Resumen {
        Long getIdUsuario();
        Integer getXpTotal();
        Integer getNivel();
        Integer getRachaActual();
        Integer getRachaMaxima();
        LocalDate getFechaUltimoEntrenamiento();
        NivelInicial getNivelInicial();
        Long getEntrenamientosSemana();
        Long getMinutosSemana();
        Long getRetosActivos();
        Long getRetosCompletados();
        Long getNotificacionesSinLeer();
    }

    @Query("""
            SELECT u.id AS idUsuario,
                   p.xpTotal AS xpTotal,
                   p.nivel AS nivel,
                   p.rachaActual AS rachaActual,
                   p.rachaMaxima AS rachaMaxima,
                   p.fechaUltimoEntrenamiento AS fechaUltimoEntrenamiento,
                   e.nivelInicial AS nivelInicial,
                   (SELECT COUNT(en) FROM Entrenamiento en
                     WHERE en.usuario = u AND en.estado = :validado
                       AND en.fechaInicio BETWEEN :desde AND :hasta) AS entrenamientosSemana,
                   (SELECT COALESCE(SUM(en.duracionMinutos), 0) FROM Entrenamiento en
                     WHERE en.usuario = u AND en.estado = :validado
                       AND en.fechaInicio BETWEEN :desde AND :hasta) AS minutosSemana,
                   (SELECT COUNT(pr) FROM ParticipacionReto pr
                     WHERE pr.usuario = u AND pr.estado = :enCurso AND pr.fechaLimite > :hasta) AS retosActivos,
                   (SELECT COUNT(pr) FROM ParticipacionReto pr
                     WHERE pr.usuario = u AND pr.estado = :completado) AS retosCompletados,
                   (SELECT COUNT(n) FROM Notificacion n
                     WHERE n.usuario = u AND n.leida = :noLeida) AS notificacionesSinLeer
            FROM Usuario u
            LEFT JOIN ProgresoJugador p ON p.usuario = u
            LEFT JOIN EvaluacionNivel e ON e.usuario = u
            WHERE u.correo = :correo
            """)
    Optional<Resumen> resumen(@Param("correo") String correo,
                              @Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta,
                              @Param("validado") EstadoEntrenamiento validado,
                              @Param("enCurso") EstadoParticipacion enCurso,
                              @Param("completado") EstadoParticipacion completado,
                              @Param("noLeida") Boolean noLeida);

    /** Resumen del dashboard entre el inicio de "desde" y "hasta". */
    default Optional<Resumen> resumen(String correo, LocalDateTime desde, LocalDateTime hasta) {
        return resumen(correo, desde, hasta, EstadoEntrenamiento.VALIDADO, EstadoParticipacion.EN_CURSO,
                EstadoParticipacion.COMPLETADO, false);
    }
}
