package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ProgresoJugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgresoJugadorRepository extends JpaRepository<ProgresoJugador, Long> {
    Optional<ProgresoJugador> findByUsuarioId(Long idUsuario);
}
