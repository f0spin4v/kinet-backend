package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.EvaluacionUsuario;
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
public interface EvaluacionUsuarioRepository extends JpaRepository<EvaluacionUsuario, Long> {

    /** La evaluación vigente es la más reciente. */
    Optional<EvaluacionUsuario> findFirstByUsuarioIdOrderByFechaEvaluacionDesc(Long idUsuario);

    Page<EvaluacionUsuario> findByUsuarioIdOrderByFechaEvaluacionDescIdDesc(Long idUsuario, Pageable pagina);

    /** Nombre del nivel vigente (la evaluación más reciente) de cada usuario. */
    @Query("""
            SELECT e.usuario.id AS id, e.nivelHabilidad.nombre AS texto FROM EvaluacionUsuario e
            WHERE e.usuario.id IN :ids
              AND e.fechaEvaluacion = (SELECT MAX(e2.fechaEvaluacion) FROM EvaluacionUsuario e2 WHERE e2.usuario = e.usuario)
            """)
    List<IdTexto> nivelesVigentes(@Param("ids") Collection<Long> idsUsuario);

    /** Si alguna evaluación eligió una opción de la pregunta (entonces sus opciones ya no se pueden cambiar). */
    @Query("SELECT COUNT(e) > 0 FROM EvaluacionUsuario e JOIN e.respuestas o WHERE o.pregunta.id = :idPregunta")
    boolean existeRespuestaDePregunta(@Param("idPregunta") Long idPregunta);

    /** Cuántos jugadores hay en cada nivel, según su evaluación vigente. */
    @Query("""
            SELECT e.nivelHabilidad.nombre AS clave, COUNT(e) AS total FROM EvaluacionUsuario e
            WHERE e.fechaEvaluacion = (SELECT MAX(e2.fechaEvaluacion) FROM EvaluacionUsuario e2 WHERE e2.usuario = e.usuario)
            GROUP BY e.nivelHabilidad.nombre
            """)
    List<Conteo> contarPorNivelVigente();
}
