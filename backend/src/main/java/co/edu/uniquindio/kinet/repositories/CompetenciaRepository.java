package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Competencia;
import co.edu.uniquindio.kinet.model.enums.EstadoCompetencia;
import co.edu.uniquindio.kinet.model.enums.EstadoInvitacion;
import co.edu.uniquindio.kinet.model.enums.TipoCompetencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    /**
     * Competencias en ciertos estados donde la invitación del jugador está en un estado dado, la más reciente
     * primero, con todos sus participantes en la misma consulta.
     */
    @Query(value = """
            SELECT c.id FROM Competencia c
            WHERE c.estado IN :estados
              AND EXISTS (SELECT 1 FROM ParticipanteCompetencia p
                          WHERE p.competencia = c AND p.usuario.id = :idUsuario AND p.estadoInvitacion = :invitacion)
            ORDER BY c.fechaCreacion DESC, c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Competencia c
            WHERE c.estado IN :estados
              AND EXISTS (SELECT 1 FROM ParticipanteCompetencia p
                          WHERE p.competencia = c AND p.usuario.id = :idUsuario AND p.estadoInvitacion = :invitacion)
            """)
    Page<Long> idsDeUsuario(@Param("idUsuario") Long idUsuario, @Param("invitacion") EstadoInvitacion invitacion,
                            @Param("estados") Collection<EstadoCompetencia> estados, Pageable pagina);

    /** Las competencias de una página de ids, cada una con todos sus participantes (sin orden: lo pone quien llama). */
    @Query("SELECT DISTINCT c FROM Competencia c JOIN FETCH c.participantes WHERE c.id IN :ids")
    List<Competencia> conParticipantes(@Param("ids") Collection<Long> ids);

    /** Cuántas competencias pendientes o en curso tiene aceptadas el jugador. */
    @Query("""
            SELECT COUNT(c) FROM Competencia c JOIN c.participantes p
            WHERE p.usuario.id = :idUsuario AND p.estadoInvitacion = :aceptado AND c.estado IN :estados
            """)
    long contarActivasDe(@Param("idUsuario") Long idUsuario, @Param("aceptado") EstadoInvitacion aceptado,
                         @Param("estados") Collection<EstadoCompetencia> estados);

    /** Grupales y torneos que ya llegaron a su hora de inicio. */
    List<Competencia> findByEstadoAndFechaInicioLessThanEqual(EstadoCompetencia estado, LocalDateTime fecha);

    /** Duelos que nadie respondió a tiempo. */
    List<Competencia> findByTipoAndEstadoAndFechaCreacionLessThanEqual(TipoCompetencia tipo, EstadoCompetencia estado,
                                                                      LocalDateTime fecha);

    /** Competencias cuya ronda en curso ya terminó. */
    List<Competencia> findByEstadoAndFechaFinLessThanEqual(EstadoCompetencia estado, LocalDateTime fecha);

    @Query("SELECT CAST(c.tipo AS string) AS clave, COUNT(c) AS total FROM Competencia c GROUP BY c.tipo")
    List<Conteo> contarPorTipo();
}
