package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Liga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LigaRepository extends JpaRepository<Liga, Long> {
    List<Liga> findAllByOrderByOrdenAsc();

    Optional<Liga> findByOrden(Integer orden);
}
