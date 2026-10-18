package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.dto.progreso.MovimientoXpDTO;
import co.edu.uniquindio.kinet.model.MovimientoXp;
import co.edu.uniquindio.kinet.model.enums.OrigenXp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface MovimientoXpRepository extends JpaRepository<MovimientoXp, Long> {

    /** Últimos movimientos del jugador, ya como DTO (sin cargar las entidades ni sus referencias). */
    @Query(value = """
            SELECT new co.edu.uniquindio.kinet.dto.progreso.MovimientoXpDTO(m.id, CAST(m.origen AS string), m.cantidad,
                   m.motivo, COALESCE(m.entrenamiento.id, m.participacionReto.id, m.logro.id, m.evidencia.id,
                   m.competencia.id), m.fechaMovimiento)
            FROM MovimientoXp m WHERE m.usuario.id = :idUsuario ORDER BY m.fechaMovimiento DESC, m.id DESC
            """,
            countQuery = "SELECT COUNT(m) FROM MovimientoXp m WHERE m.usuario.id = :idUsuario")
    Page<MovimientoXpDTO> ultimos(@Param("idUsuario") Long idUsuario, Pageable pagina);

    Optional<MovimientoXp> findByEntrenamientoId(Long idEntrenamiento);

    /** XP que dio cada entrenamiento. */
    @Query("SELECT m.entrenamiento.id AS id, m.cantidad AS valor FROM MovimientoXp m WHERE m.entrenamiento.id IN :ids")
    List<IdValor> xpDeEntrenamientos(@Param("ids") Collection<Long> idsEntrenamiento);

    Optional<MovimientoXp> findByEvidenciaId(Long idEvidencia);

    boolean existsByEvidenciaId(Long idEvidencia);

    /**
     * XP de entrenamientos que empezaron dentro de una ventana, por jugador (puntaje de enfrentamientos y
     * competencias). Una sola consulta para todos los participantes. Solo cuentan los entrenamientos con un
     * enlace de evidencia verificado que nadie rechazó: competir exige esa prueba.
     */
    @Query("""
            SELECT m.usuario.id AS id, SUM(m.cantidad) AS valor FROM MovimientoXp m
            WHERE m.usuario.id IN :ids AND m.origen = :origen
              AND m.entrenamiento.fechaHoraInicio >= :desde AND m.entrenamiento.fechaHoraInicio < :hasta
              AND EXISTS (SELECT 1 FROM EvidenciaEntrenamiento ev WHERE ev.entrenamiento = m.entrenamiento
                          AND ev.urlExterna IS NOT NULL
                          AND ev.estadoRevision <> co.edu.uniquindio.kinet.model.enums.EstadoEvidencia.RECHAZADA)
            GROUP BY m.usuario.id
            """)
    List<IdValor> xpEnVentanaPorUsuario(@Param("ids") Collection<Long> idsUsuario, @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta, @Param("origen") OrigenXp origen);

    /** Igual, pero solo de una actividad (competencias de una actividad). */
    @Query("""
            SELECT m.usuario.id AS id, SUM(m.cantidad) AS valor FROM MovimientoXp m
            WHERE m.usuario.id IN :ids AND m.origen = :origen AND m.entrenamiento.tipoActividad.id = :idActividad
              AND m.entrenamiento.fechaHoraInicio >= :desde AND m.entrenamiento.fechaHoraInicio < :hasta
              AND EXISTS (SELECT 1 FROM EvidenciaEntrenamiento ev WHERE ev.entrenamiento = m.entrenamiento
                          AND ev.urlExterna IS NOT NULL
                          AND ev.estadoRevision <> co.edu.uniquindio.kinet.model.enums.EstadoEvidencia.RECHAZADA)
            GROUP BY m.usuario.id
            """)
    List<IdValor> xpEnVentanaPorUsuarioDeActividad(@Param("ids") Collection<Long> idsUsuario,
                                                   @Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta,
                                                   @Param("idActividad") Long idActividad,
                                                   @Param("origen") OrigenXp origen);

    /** XP de entrenamientos en la ventana [desde, hasta) por jugador; idActividad null = cualquier actividad. */
    default Map<Long, Long> xpEnVentana(Collection<Long> idsUsuario, LocalDateTime desde, LocalDateTime hasta,
                                        Long idActividad) {
        if (idsUsuario.isEmpty()) {
            return Map.of();
        }
        List<IdValor> filas = idActividad == null
                ? xpEnVentanaPorUsuario(idsUsuario, desde, hasta, OrigenXp.ENTRENAMIENTO)
                : xpEnVentanaPorUsuarioDeActividad(idsUsuario, desde, hasta, idActividad, OrigenXp.ENTRENAMIENTO);
        return filas.stream().collect(Collectors.toMap(IdValor::getId, IdValor::getValor));
    }

    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoXp m WHERE m.usuario.id = :idUsuario")
    long saldo(@Param("idUsuario") Long idUsuario);

    @Query("""
            SELECT CAST(m.origen AS string) AS clave, SUM(m.cantidad) AS total FROM MovimientoXp m
            WHERE m.fechaMovimiento >= :desde GROUP BY m.origen
            """)
    List<Conteo> xpPorOrigenDesde(@Param("desde") LocalDateTime desde);
}
