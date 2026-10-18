package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.ResultadoEnfrentamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilJugadorRepository extends JpaRepository<PerfilJugador, Long> {

    boolean existsByGamertag(String gamertag);

    boolean existsByGamertagAndIdNot(String gamertag, Long idUsuario);

    /** Valores con los que se evalúan los criterios de los logros. */
    interface MetricasLogro {
        Long getEntrenamientos();

        Long getMinutos();

        Integer getRacha();

        Integer getXp();

        Long getRetos();

        Long getVictorias();

        Long getClanes();
    }

    /** Todas las métricas de logros del jugador en una sola consulta. */
    @Query("""
            SELECT (SELECT COUNT(e) FROM Entrenamiento e
                    WHERE e.usuario.id = p.id AND e.estadoValidacion = :validado) AS entrenamientos,
                   (SELECT COALESCE(SUM(e.duracionMin), 0) FROM Entrenamiento e
                    WHERE e.usuario.id = p.id AND e.estadoValidacion = :validado) AS minutos,
                   p.rachaMaxima AS racha,
                   p.xpTotal AS xp,
                   (SELECT COUNT(r) FROM ParticipacionReto r WHERE r.usuario.id = p.id AND r.estado = :completado) AS retos,
                   (SELECT COUNT(pe) FROM ParticipanteEnfrentamiento pe
                    WHERE pe.usuario.id = p.id AND pe.resultado = :victoria) AS victorias,
                   (SELECT COUNT(m) FROM MembresiaClan m WHERE m.usuario.id = p.id) AS clanes
            FROM PerfilJugador p WHERE p.id = :idUsuario
            """)
    MetricasLogro metricasLogro(@Param("idUsuario") Long idUsuario, @Param("validado") EstadoValidacion validado,
                                @Param("completado") EstadoParticipacion completado,
                                @Param("victoria") ResultadoEnfrentamiento victoria);

    /** Gamertag de varios usuarios a la vez (evita una consulta por fila al armar listas). */
    @Query("SELECT p.id AS id, p.gamertag AS texto FROM PerfilJugador p WHERE p.id IN :ids")
    List<IdTexto> gamertags(@Param("ids") Collection<Long> idsUsuario);

    /** Datos del dashboard (UI-003) en una sola consulta. */
    interface Resumen {
        String getGamertag();
        Integer getXpTotal();
        Integer getRachaActual();
        Integer getRachaMaxima();
        LocalDate getFechaUltimaActividad();
        Integer getNivel();
        Integer getXpInicioNivel();
        Integer getXpSiguienteNivel();
        String getNivelHabilidad();
        Long getEntrenamientosSemana();
        Long getMinutosSemana();
        Long getRetosActivos();
        Long getRetosCompletados();
        Long getLogros();
        Long getNotificacionesSinLeer();
        String getTemporada();
        Integer getPuntosTemporada();
        String getLiga();
        String getDivision();
    }

    @Query("""
            SELECT p.gamertag AS gamertag,
                   p.xpTotal AS xpTotal,
                   p.rachaActual AS rachaActual,
                   p.rachaMaxima AS rachaMaxima,
                   p.fechaUltimaActividad AS fechaUltimaActividad,
                   (SELECT MAX(np.numeroNivel) FROM NivelProgresion np WHERE np.xpRequerido <= p.xpTotal) AS nivel,
                   (SELECT MAX(np.xpRequerido) FROM NivelProgresion np WHERE np.xpRequerido <= p.xpTotal) AS xpInicioNivel,
                   (SELECT MIN(np.xpRequerido) FROM NivelProgresion np WHERE np.xpRequerido > p.xpTotal) AS xpSiguienteNivel,
                   (SELECT nh.nombre FROM EvaluacionUsuario ev JOIN ev.nivelHabilidad nh
                     WHERE ev.usuario = u AND ev.fechaEvaluacion =
                           (SELECT MAX(ev2.fechaEvaluacion) FROM EvaluacionUsuario ev2 WHERE ev2.usuario = u)) AS nivelHabilidad,
                   (SELECT COUNT(e) FROM Entrenamiento e
                     WHERE e.usuario = u AND e.estadoValidacion = :validado AND e.fechaHoraInicio >= :desde) AS entrenamientosSemana,
                   (SELECT COALESCE(SUM(e.duracionMin), 0) FROM Entrenamiento e
                     WHERE e.usuario = u AND e.estadoValidacion = :validado AND e.fechaHoraInicio >= :desde) AS minutosSemana,
                   (SELECT COUNT(pr) FROM ParticipacionReto pr
                     WHERE pr.usuario = u AND pr.estado = :enCurso AND pr.fechaLimite > :ahora) AS retosActivos,
                   (SELECT COUNT(pr) FROM ParticipacionReto pr
                     WHERE pr.usuario = u AND pr.estado = :completado) AS retosCompletados,
                   (SELECT COUNT(lu) FROM LogroUsuario lu WHERE lu.usuario = u) AS logros,
                   (SELECT COUNT(n) FROM Notificacion n WHERE n.usuario = u AND n.fechaLectura IS NULL) AS notificacionesSinLeer,
                   t.nombre AS temporada,
                   pt.puntosActuales AS puntosTemporada,
                   l.nombre AS liga,
                   r.division AS division
            FROM PerfilJugador p
            JOIN p.usuario u
            LEFT JOIN Temporada t ON t.estado = :activa
            LEFT JOIN ParticipacionTemporada pt ON pt.usuario = u AND pt.temporada = t
            LEFT JOIN pt.rangoActual r
            LEFT JOIN r.liga l
            WHERE u.correo = :correo
            """)
    Optional<Resumen> resumen(@Param("correo") String correo,
                              @Param("desde") LocalDateTime desde,
                              @Param("ahora") LocalDateTime ahora,
                              @Param("validado") EstadoValidacion validado,
                              @Param("enCurso") EstadoParticipacion enCurso,
                              @Param("completado") EstadoParticipacion completado,
                              @Param("activa") EstadoTemporada activa);

    default Optional<Resumen> resumen(String correo, LocalDateTime desde, LocalDateTime ahora) {
        return resumen(correo, desde, ahora, EstadoValidacion.VALIDADO, EstadoParticipacion.EN_CURSO,
                EstadoParticipacion.COMPLETADO, EstadoTemporada.ACTIVA);
    }
}
