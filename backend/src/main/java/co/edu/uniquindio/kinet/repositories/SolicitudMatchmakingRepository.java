package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.SolicitudMatchmaking;
import co.edu.uniquindio.kinet.model.enums.EstadoEnfrentamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacionTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoSolicitud;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudMatchmakingRepository extends JpaRepository<SolicitudMatchmaking, Long> {
    Optional<SolicitudMatchmaking> findFirstByUsuarioIdAndTemporadaIdAndEstado(Long idUsuario, Long idTemporada,
                                                                            EstadoSolicitud estado);

    /**
     * Rivales posibles en la cola: participación ACTIVA y sin enfrentamiento en curso, el de rango más parecido
     * primero y, en empate, el que lleva más tiempo esperando. Con una página de tamaño 1 da el mejor rival.
     */
    @Query("""
            SELECT s FROM SolicitudMatchmaking s
                 JOIN ParticipacionTemporada pt ON pt.usuario = s.usuario AND pt.temporada = s.temporada
            WHERE s.temporada.id = :idTemporada AND s.estado = :enCola AND s.usuario.id <> :idUsuario
              AND pt.estado = :activa
              AND NOT EXISTS (SELECT 1 FROM ParticipanteEnfrentamiento pe
                              WHERE pe.usuario = s.usuario AND pe.enfrentamiento.estado = :enCurso)
            ORDER BY ABS(pt.rangoActual.orden - :orden), s.fechaSolicitud
            """)
    List<SolicitudMatchmaking> rivales(@Param("idTemporada") Long idTemporada, @Param("idUsuario") Long idUsuario,
                                       @Param("orden") Integer ordenRango, @Param("enCola") EstadoSolicitud enCola,
                                       @Param("activa") EstadoParticipacionTemporada activa,
                                       @Param("enCurso") EstadoEnfrentamiento enCurso, Pageable pagina);

    /** Vacía la cola de una temporada (al cerrarla). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE SolicitudMatchmaking s SET s.estado = :nuevo WHERE s.temporada.id = :idTemporada AND s.estado = :actual")
    int cambiarEstadoDeTemporada(@Param("idTemporada") Long idTemporada, @Param("actual") EstadoSolicitud actual,
                                 @Param("nuevo") EstadoSolicitud nuevo);

    /** Saca a un jugador de la cola (al abandonar la temporada o ser descalificado). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE SolicitudMatchmaking s SET s.estado = :nuevo
            WHERE s.temporada.id = :idTemporada AND s.usuario.id = :idUsuario AND s.estado = :actual
            """)
    int cambiarEstadoDeUsuario(@Param("idTemporada") Long idTemporada, @Param("idUsuario") Long idUsuario,
                               @Param("actual") EstadoSolicitud actual, @Param("nuevo") EstadoSolicitud nuevo);
}
