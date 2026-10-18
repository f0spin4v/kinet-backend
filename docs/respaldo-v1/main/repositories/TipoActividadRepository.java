package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoActividadRepository extends JpaRepository<TipoActividad, Long> {
    Optional<TipoActividad> findByCodigo(String codigo);
    List<TipoActividad> findAllByOrderByNombreAsc();
}
