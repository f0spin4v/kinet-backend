package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.PreguntaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreguntaEvaluacionRepository extends JpaRepository<PreguntaEvaluacion, Long> {
    List<PreguntaEvaluacion> findByActivaOrderByOrdenAsc(Boolean activa);

    List<PreguntaEvaluacion> findAllByOrderByOrdenAsc();

    long countByActiva(Boolean activa);

    boolean existsByOrden(Integer orden);

    boolean existsByOrdenAndIdNot(Integer orden, Long id);
}
