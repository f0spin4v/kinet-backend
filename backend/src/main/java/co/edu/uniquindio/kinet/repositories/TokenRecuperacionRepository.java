package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);
    List<TokenRecuperacion> findByUsuarioIdAndUsado(Long idUsuario, Boolean usado);
}
