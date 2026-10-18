package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Enfrentamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoEnfrentamiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnfrentamientoRepository extends JpaRepository<Enfrentamiento, Long> {

    List<Enfrentamiento> findByEstadoAndFechaFinLessThanEqual(EstadoEnfrentamiento estado, LocalDateTime fecha);

    /** Enfrentamientos del jugador con su temporada y sus participantes, en una sola consulta. */
    @Query(value = """
            SELECT e.id FROM Enfrentamiento e
            WHERE EXISTS (SELECT 1 FROM ParticipanteEnfrentamiento p WHERE p.enfrentamiento = e AND p.usuario.id = :idUsuario)
            ORDER BY e.fechaInicio DESC, e.id DESC
            """,
            countQuery = """
            SELECT COUNT(e) FROM Enfrentamiento e
            WHERE EXISTS (SELECT 1 FROM ParticipanteEnfrentamiento p WHERE p.enfrentamiento = e AND p.usuario.id = :idUsuario)
            """)
    Page<Long> idsDeUsuario(@Param("idUsuario") Long idUsuario, Pageable pagina);

    /** Los enfrentamientos de una página de ids, con su temporada y sus participantes (sin orden: lo pone quien llama). */
    @Query("SELECT DISTINCT e FROM Enfrentamiento e JOIN FETCH e.temporada JOIN FETCH e.participantes WHERE e.id IN :ids")
    List<Enfrentamiento> conParticipantes(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT COUNT(e) > 0 FROM Enfrentamiento e JOIN e.participantes p
            WHERE p.usuario.id = :idUsuario AND e.estado = :estado
            """)
    boolean existeDeUsuarioEnEstado(@Param("idUsuario") Long idUsuario, @Param("estado") EstadoEnfrentamiento estado);

    @Query("""
            SELECT COUNT(e) FROM Enfrentamiento e JOIN e.participantes p
            WHERE p.usuario.id = :idUsuario AND e.temporada.id = :idTemporada AND e.estado <> :cancelado
            """)
    long contarDeUsuarioEnTemporada(@Param("idUsuario") Long idUsuario, @Param("idTemporada") Long idTemporada,
                                    @Param("cancelado") EstadoEnfrentamiento cancelado);

    @Query("""
            SELECT e FROM Enfrentamiento e JOIN e.participantes p
            WHERE e.id = :id AND p.usuario.id = :idUsuario
            """)
    Optional<Enfrentamiento> deUsuarioPorId(@Param("id") Long id, @Param("idUsuario") Long idUsuario);

    /** Al cerrar la temporada: los enfrentamientos en curso se cancelan en bloque. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Enfrentamiento e SET e.estado = :nuevo WHERE e.temporada.id = :idTemporada AND e.estado = :actual")
    int cambiarEstadoDeTemporada(@Param("idTemporada") Long idTemporada, @Param("actual") EstadoEnfrentamiento actual,
                                 @Param("nuevo") EstadoEnfrentamiento nuevo);
}
