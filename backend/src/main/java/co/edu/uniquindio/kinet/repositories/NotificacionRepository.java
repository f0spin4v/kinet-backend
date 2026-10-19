package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Notificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    Page<Notificacion> findByUsuarioIdOrderByFechaCreacionDescIdDesc(Long idUsuario, Pageable pagina);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long idUsuario);

    @Modifying
    @Query("UPDATE Notificacion n SET n.fechaLectura = :ahora WHERE n.usuario.id = :idUsuario AND n.fechaLectura IS NULL")
    int marcarTodas(@Param("idUsuario") Long idUsuario, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.usuario.id = :idUsuario")
    int borrarTodas(@Param("idUsuario") Long idUsuario);

    /** Resultado del envío, sin volver a leer la fila. */
    @Modifying
    @Query("UPDATE Notificacion n SET n.estadoEnvio = :estado, n.fechaEnvio = :fecha WHERE n.id = :id")
    int marcarEnvio(@Param("id") Long id, @Param("estado") EstadoEnvio estado, @Param("fecha") LocalDateTime fecha);

    @Query("SELECT CAST(n.estadoEnvio AS string) AS clave, COUNT(n) AS total FROM Notificacion n GROUP BY n.estadoEnvio")
    List<Conteo> contarPorEstadoEnvio();
}
