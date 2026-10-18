package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.AporteReto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AporteRetoRepository extends JpaRepository<AporteReto, Long> {
}
