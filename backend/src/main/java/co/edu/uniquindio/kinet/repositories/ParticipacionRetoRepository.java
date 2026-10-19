package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ParticipacionReto;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.MetricaReto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacionRetoRepository extends JpaRepository<ParticipacionReto, Long> {
    Optional<ParticipacionReto> findByRetoIdAndUsuarioIdAndPeriodo(Long idReto, Long idUsuario, LocalDate periodo);

    @EntityGraph(attributePaths = "reto")
    Page<ParticipacionReto> findByUsuarioIdOrderByFechaInscripcionDescIdDesc(Long idUsuario, Pageable pagina);

    /** Participaciones del jugador en varios retos (para mostrar el avance en la lista de retos). */
    List<ParticipacionReto> findByUsuarioIdAndRetoIdIn(Long idUsuario, Collection<Long> idsReto);

    /**
     * Participaciones en curso donde cuenta un entrenamiento validado: empezó dentro del periodo, terminó después
     * de la inscripción, el reto se mide con entrenamientos (no REPETICIONES), es de la actividad pedida (si la hay)
     * y el entrenamiento todavía no aportó.
     */
    @Query("""
            SELECT p FROM ParticipacionReto p JOIN FETCH p.reto r
            WHERE p.usuario.id = :idUsuario AND p.estado = :enCurso
              AND p.fechaInscripcion <= :fin AND p.periodo <= :dia AND p.fechaLimite > :inicio
              AND r.metrica IN :metricas
              AND (r.tipoActividad IS NULL OR r.tipoActividad.id = :idActividad)
              AND NOT EXISTS (SELECT 1 FROM AporteReto a WHERE a.participacion = p AND a.entrenamiento.id = :idEntrenamiento)
            """)
    List<ParticipacionReto> dondeCuenta(@Param("idUsuario") Long idUsuario, @Param("idEntrenamiento") Long idEntrenamiento,
                                        @Param("idActividad") Long idActividad, @Param("inicio") LocalDateTime inicio,
                                        @Param("dia") LocalDate dia, @Param("fin") LocalDateTime fin,
                                        @Param("metricas") Collection<MetricaReto> metricas,
                                        @Param("enCurso") EstadoParticipacion enCurso);

    Optional<ParticipacionReto> findByIdAndUsuarioId(Long id, Long idUsuario);

    /** Participaciones de los miembros de un clan en un reto y periodo. */
    List<ParticipacionReto> findByRetoIdAndClanIdAndPeriodoAndEstado(Long idReto, Long idClan, LocalDate periodo,
                                                                     EstadoParticipacion estado);

    @Query("""
            SELECT COALESCE(SUM(p.progreso), 0) FROM ParticipacionReto p
            WHERE p.reto.id = :idReto AND p.clan.id = :idClan AND p.periodo = :periodo
            """)
    BigDecimal progresoDelClan(@Param("idReto") Long idReto, @Param("idClan") Long idClan,
                               @Param("periodo") LocalDate periodo);

    // ---------- cambios de estado en bloque ----------

    /** Vence las participaciones en curso cuyo plazo pasó (de todos, o de un jugador si idUsuario no es null). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ParticipacionReto p SET p.estado = :vencido
            WHERE p.estado = :enCurso AND p.fechaLimite <= :ahora
            """)
    int vencerTodas(@Param("ahora") LocalDateTime ahora, @Param("enCurso") EstadoParticipacion enCurso,
                    @Param("vencido") EstadoParticipacion vencido);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ParticipacionReto p SET p.estado = :vencido
            WHERE p.usuario.id = :idUsuario AND p.estado = :enCurso AND p.fechaLimite <= :ahora
            """)
    int vencerDe(@Param("idUsuario") Long idUsuario, @Param("ahora") LocalDateTime ahora,
                 @Param("enCurso") EstadoParticipacion enCurso, @Param("vencido") EstadoParticipacion vencido);

    /** Al finalizar un reto: sus participaciones en curso vencen. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ParticipacionReto p SET p.estado = :nuevo WHERE p.reto.id = :idReto AND p.estado = :actual")
    int cambiarEstadoDeReto(@Param("idReto") Long idReto, @Param("actual") EstadoParticipacion actual,
                            @Param("nuevo") EstadoParticipacion nuevo);

    /** Al cerrar un clan: los retos de clan en curso de sus miembros quedan abandonados. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ParticipacionReto p SET p.estado = :nuevo WHERE p.clan.id = :idClan AND p.estado = :actual")
    int cambiarEstadoDeClan(@Param("idClan") Long idClan, @Param("actual") EstadoParticipacion actual,
                            @Param("nuevo") EstadoParticipacion nuevo);

    /** Cuando un miembro sale del clan: sus retos de clan en curso quedan abandonados. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ParticipacionReto p SET p.estado = :nuevo
            WHERE p.clan.id = :idClan AND p.usuario.id = :idUsuario AND p.estado = :actual
            """)
    int cambiarEstadoDeMiembro(@Param("idClan") Long idClan, @Param("idUsuario") Long idUsuario,
                               @Param("actual") EstadoParticipacion actual, @Param("nuevo") EstadoParticipacion nuevo);

    @Query("""
            SELECT p.reto.nombre AS clave, COUNT(p) AS total FROM ParticipacionReto p
            WHERE p.estado = :estado AND p.fechaCompletado >= :desde
            GROUP BY p.reto.nombre ORDER BY COUNT(p) DESC
            """)
    List<Conteo> completadosPorRetoDesde(@Param("estado") EstadoParticipacion estado,
                                         @Param("desde") LocalDateTime desde);
}
