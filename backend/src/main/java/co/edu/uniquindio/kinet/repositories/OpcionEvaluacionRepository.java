package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.OpcionEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpcionEvaluacionRepository extends JpaRepository<OpcionEvaluacion, Long> {
}
