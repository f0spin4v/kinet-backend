package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop30ByUsuarioIdOrderByFechaDesc(Long idUsuario);
    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long idUsuario);
    long countByUsuarioIdAndLeida(Long idUsuario, Boolean leida);
}
