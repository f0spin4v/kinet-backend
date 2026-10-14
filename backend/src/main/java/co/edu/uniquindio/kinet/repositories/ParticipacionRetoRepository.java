package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ParticipacionReto;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacionRetoRepository extends JpaRepository<ParticipacionReto, Long> {

    List<ParticipacionReto> findByUsuarioIdAndEstado(Long idUsuario, EstadoParticipacion estado);

    List<ParticipacionReto> findByUsuarioIdOrderByFechaInicioDesc(Long idUsuario);

    Optional<ParticipacionReto> findFirstByUsuarioIdAndRetoIdOrderByFechaInicioDesc(Long idUsuario, Long idReto);

    boolean existsByUsuarioIdAndRetoIdAndEstado(Long idUsuario, Long idReto, EstadoParticipacion estado);

    long countByUsuarioIdAndEstado(Long idUsuario, EstadoParticipacion estado);

    long countByUsuarioIdAndEstadoAndFechaLimiteAfter(Long idUsuario, EstadoParticipacion estado, java.time.LocalDateTime fecha);
}
