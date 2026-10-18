package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Reto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetoRepository extends JpaRepository<Reto, Long> {
    List<Reto> findByActivoOrderByXpRecompensaAsc(Boolean activo);
}
