package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Un enlace nuevo invalida los anteriores que seguían sin usar. */
    @Modifying
    @Query("UPDATE TokenRecuperacion t SET t.fechaUso = :ahora WHERE t.usuario.id = :idUsuario AND t.fechaUso IS NULL")
    int invalidarPendientes(@Param("idUsuario") Long idUsuario, @Param("ahora") LocalDateTime ahora);
}
