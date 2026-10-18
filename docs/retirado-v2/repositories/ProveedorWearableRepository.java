package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ProveedorWearable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorWearableRepository extends JpaRepository<ProveedorWearable, Long> {
    List<ProveedorWearable> findAllByOrderByNombreAsc();

    boolean existsByNombre(String nombre);
}
