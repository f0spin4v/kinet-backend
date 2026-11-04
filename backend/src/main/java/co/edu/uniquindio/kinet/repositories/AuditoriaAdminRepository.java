package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.AuditoriaAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaAdminRepository extends JpaRepository<AuditoriaAdmin, Long> {

    /** Lo más reciente primero. */
    Page<AuditoriaAdmin> findAllByOrderByFechaDescIdDesc(Pageable pagina);
}
