package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.NivelHabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NivelHabilidadRepository extends JpaRepository<NivelHabilidad, Long> {
    List<NivelHabilidad> findAllByOrderByOrdenAsc();

    Optional<NivelHabilidad> findByNombre(String nombre);
}
