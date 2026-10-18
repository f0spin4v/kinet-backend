package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.dto.entrenamiento.EvidenciaDTO;
import co.edu.uniquindio.kinet.model.EvidenciaEntrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenciaEntrenamientoRepository extends JpaRepository<EvidenciaEntrenamiento, Long> {

    boolean existsByEntrenamientoId(Long idEntrenamiento);

    Optional<EvidenciaEntrenamiento> findByEntrenamientoId(Long idEntrenamiento);

    boolean existsByUrlExterna(String url);

    /** ¿El entrenamiento tiene un enlace de evidencia que nadie rechazó? Es la prueba que piden las sesiones largas. */
    boolean existsByEntrenamientoIdAndUrlExternaIsNotNullAndEstadoRevisionNot(Long idEntrenamiento,
                                                                              EstadoEvidencia estado);

    String DATOS = """
            SELECT new co.edu.uniquindio.kinet.dto.entrenamiento.EvidenciaDTO(
                       ev.id, en.id, u.id, p.gamertag, ta.nombre, en.duracionMin, en.estadoValidacion,
                       CASE WHEN ev.urlExterna IS NULL THEN 'IMAGEN' ELSE 'ENLACE' END,
                       ev.nombreArchivo, ev.tipoContenido, ev.tamanoBytes, ev.urlExterna, pl.nombre,
                       ev.estadoRevision, ev.notaRevision, ev.fechaSubida, ev.fechaRevision)
            FROM EvidenciaEntrenamiento ev
            JOIN ev.entrenamiento en
            JOIN en.usuario u
            JOIN PerfilJugador p ON p.usuario = u
            JOIN en.tipoActividad ta
            LEFT JOIN ev.plataforma pl
            """;

    /** Datos de las evidencias sin cargar las imágenes; estado null = todas. */
    @Query(value = DATOS + " WHERE (:estado IS NULL OR ev.estadoRevision = :estado) ORDER BY ev.fechaSubida ASC, ev.id ASC",
            countQuery = "SELECT COUNT(ev) FROM EvidenciaEntrenamiento ev WHERE (:estado IS NULL OR ev.estadoRevision = :estado)")
    Page<EvidenciaDTO> datos(@Param("estado") EstadoEvidencia estado, Pageable pagina);

    @Query(DATOS + " WHERE ev.id = :id")
    Optional<EvidenciaDTO> datosPorId(@Param("id") Long id);

    /** Estado de la evidencia de cada entrenamiento de la lista (sin cargar imágenes). */
    @Query("SELECT ev.entrenamiento.id, ev.estadoRevision FROM EvidenciaEntrenamiento ev WHERE ev.entrenamiento.id IN :ids")
    List<Object[]> estadosDe(@Param("ids") Collection<Long> idsEntrenamiento);
}
