package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.TipoActividad;
import co.edu.uniquindio.kinet.model.enums.EstadoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoActividadRepository extends JpaRepository<TipoActividad, Long> {
    List<TipoActividad> findAllByOrderByNombreAsc();

    List<TipoActividad> findByEstadoOrderByNombreAsc(EstadoActividad estado);

    Optional<TipoActividad> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
