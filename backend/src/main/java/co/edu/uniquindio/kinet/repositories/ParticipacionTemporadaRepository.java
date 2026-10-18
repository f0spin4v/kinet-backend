package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ParticipacionTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacionTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface ParticipacionTemporadaRepository
        extends JpaRepository<ParticipacionTemporada, ParticipacionTemporada.Id> {

    /** La participación con sus rangos (y sus ligas) en una sola consulta. */
    @EntityGraph(attributePaths = {"rangoActual", "rangoActual.liga", "rangoInicial", "rangoInicial.liga"})
    Optional<ParticipacionTemporada> findByUsuarioIdAndTemporadaId(Long idUsuario, Long idTemporada);

    /** Ranking: más puntos primero; en empate, quien se inscribió antes. */
    List<ParticipacionTemporada> findByTemporadaIdAndEstadoInOrderByPuntosActualesDescFechaInscripcionAsc(
            Long idTemporada, Collection<EstadoParticipacionTemporada> estados);

    /** Última temporada cerrada en la que participó el jugador. */
    Optional<ParticipacionTemporada> findFirstByUsuarioIdAndTemporadaEstadoOrderByTemporadaFechaFinDesc(
            Long idUsuario, EstadoTemporada estado);

    /** Fila del ranking: jugador, gamertag, puntos y rango, sin cargar entidades. */
    interface FilaRanking {
        Long getIdUsuario();

        String getGamertag();

        Integer getPuntos();

        String getLiga();

        String getDivision();
    }

    /** Ranking de una temporada (los "pagina.size" primeros) en una sola consulta. */
    @Query("""
            SELECT pt.usuario.id AS idUsuario, p.gamertag AS gamertag, pt.puntosActuales AS puntos,
                   l.nombre AS liga, r.division AS division
            FROM ParticipacionTemporada pt JOIN PerfilJugador p ON p.id = pt.usuario.id
                 JOIN pt.rangoActual r JOIN r.liga l
            WHERE pt.temporada.id = :idTemporada AND pt.estado IN :estados
            ORDER BY pt.puntosActuales DESC, pt.fechaInscripcion ASC
            """)
    List<FilaRanking> ranking(@Param("idTemporada") Long idTemporada,
                              @Param("estados") Collection<EstadoParticipacionTemporada> estados, Pageable pagina);

    /** Cuántos terminaron (FINALIZADA) una temporada en una liga. */
    @Query("""
            SELECT COUNT(pt) FROM ParticipacionTemporada pt
            WHERE pt.temporada.id = :idTemporada AND pt.rangoActual.liga.id = :idLiga AND pt.estado = :finalizada
            """)
    long contarFinalizadosEnLiga(@Param("idTemporada") Long idTemporada, @Param("idLiga") Long idLiga,
                                 @Param("finalizada") EstadoParticipacionTemporada finalizada);

    /**
     * Cuántos quedaron por delante en la liga al cerrar (más puntos finales o, con los mismos, inscritos antes):
     * es la posición del jugador en su liga, empezando en 0.
     */
    @Query("""
            SELECT COUNT(pt) FROM ParticipacionTemporada pt
            WHERE pt.temporada.id = :idTemporada AND pt.rangoActual.liga.id = :idLiga AND pt.estado = :finalizada
              AND (pt.puntosFinales > :puntos OR (pt.puntosFinales = :puntos AND pt.fechaInscripcion < :inscripcion))
            """)
    long adelanteEnLiga(@Param("idTemporada") Long idTemporada, @Param("idLiga") Long idLiga,
                        @Param("puntos") Integer puntosFinales, @Param("inscripcion") LocalDateTime inscripcion,
                        @Param("finalizada") EstadoParticipacionTemporada finalizada);

    /** Al cerrar la temporada, quien abandonó conserva sus puntos como finales (sin posición). */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ParticipacionTemporada pt SET pt.puntosFinales = pt.puntosActuales
            WHERE pt.temporada.id = :idTemporada AND pt.estado = :estado
            """)
    int fijarPuntosFinales(@Param("idTemporada") Long idTemporada, @Param("estado") EstadoParticipacionTemporada estado);

    long countByTemporadaIdAndEstadoAndPuntosActualesGreaterThan(Long idTemporada, EstadoParticipacionTemporada estado,
                                                                 Integer puntos);

    @Query("""
            SELECT pt.rangoActual.liga.nombre AS clave, COUNT(pt) AS total FROM ParticipacionTemporada pt
            WHERE pt.temporada.estado = :estado GROUP BY pt.rangoActual.liga.nombre
            """)
    List<Conteo> contarPorLiga(@Param("estado") EstadoTemporada estado);
}
