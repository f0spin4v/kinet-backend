package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {
    Page<Entrenamiento> findByUsuarioIdOrderByFechaHoraInicioDescIdDesc(Long idUsuario, Pageable pagina);

    Optional<Entrenamiento> findByIdAndUsuarioId(Long id, Long idUsuario);

    /** El cronómetro en marcha del jugador (como máximo uno). */
    Optional<Entrenamiento> findFirstByUsuarioIdAndEstadoValidacion(Long idUsuario, EstadoValidacion estado);

    /** Sesiones y minutos de un día que cuentan para los topes diarios. */
    interface TotalesDia {
        Long getSesiones();

        Long getMinutos();
    }

    @Query("""
            SELECT COUNT(e) AS sesiones, COALESCE(SUM(e.duracionMin), 0) AS minutos FROM Entrenamiento e
            WHERE e.usuario.id = :idUsuario AND e.estadoValidacion IN :estados
              AND e.fechaHoraInicio >= :desde AND e.fechaHoraInicio < :hasta
            """)
    TotalesDia totalesDelDia(@Param("idUsuario") Long idUsuario, @Param("estados") Collection<EstadoValidacion> estados,
                             @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Cronómetros que se quedaron corriendo desde antes de "limite". */
    List<Entrenamiento> findByEstadoValidacionAndFechaHoraInicioBefore(EstadoValidacion estado, LocalDateTime limite);

    /** Sesiones en un estado, terminadas antes de "limite", sin un enlace de evidencia que no esté rechazado. */
    @Query("""
            SELECT e FROM Entrenamiento e
            WHERE e.estadoValidacion = :estado AND e.fechaHoraFin < :limite
              AND NOT EXISTS (SELECT 1 FROM EvidenciaEntrenamiento ev
                              WHERE ev.entrenamiento = e AND ev.urlExterna IS NOT NULL
                                AND ev.estadoRevision <> :rechazada)
            """)
    List<Entrenamiento> sinEnlaceTerminadasAntesDe(@Param("estado") EstadoValidacion estado,
                                                   @Param("limite") LocalDateTime limite,
                                                   @Param("rechazada") EstadoEvidencia rechazada);

    Page<Entrenamiento> findByEstadoValidacionOrderByFechaRegistroAscIdAsc(EstadoValidacion estado, Pageable pagina);

    @Query("""
            SELECT CAST(e.estadoValidacion AS string) AS clave, COUNT(e) AS total FROM Entrenamiento e
            WHERE e.fechaRegistro >= :desde GROUP BY e.estadoValidacion
            """)
    List<Conteo> contarPorEstadoDesde(@Param("desde") LocalDateTime desde);

    @Query("""
            SELECT e.tipoActividad.nombre AS clave, SUM(e.duracionMin) AS total FROM Entrenamiento e
            WHERE e.estadoValidacion = :estado AND e.fechaHoraInicio >= :desde
            GROUP BY e.tipoActividad.nombre ORDER BY SUM(e.duracionMin) DESC
            """)
    List<Conteo> minutosPorActividadDesde(@Param("estado") EstadoValidacion estado,
                                          @Param("desde") LocalDateTime desde);
}
