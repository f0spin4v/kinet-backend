package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ConexionWearable;
import co.edu.uniquindio.kinet.model.enums.EstadoConexion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConexionWearableRepository extends JpaRepository<ConexionWearable, Long> {
    List<ConexionWearable> findByUsuarioIdOrderByFechaVinculacionDesc(Long idUsuario);

    Optional<ConexionWearable> findByIdAndUsuarioId(Long id, Long idUsuario);

    boolean existsByUsuarioIdAndProveedorIdAndEstado(Long idUsuario, Long idProveedor, EstadoConexion estado);

    boolean existsByProveedorIdAndIdCuentaExternaAndEstado(Long idProveedor, String idCuentaExterna,
                                                           EstadoConexion estado);
}
