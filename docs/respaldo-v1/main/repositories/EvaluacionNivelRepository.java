package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.EvaluacionNivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluacionNivelRepository extends JpaRepository<EvaluacionNivel, Long> {
    Optional<EvaluacionNivel> findByUsuarioId(Long idUsuario);
    boolean existsByUsuarioId(Long idUsuario);
}
