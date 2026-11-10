package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.AutorizacionMenor;
import co.edu.uniquindio.kinet.model.enums.EstadoAutorizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutorizacionMenorRepository extends JpaRepository<AutorizacionMenor, Long> {

    @Query("SELECT a FROM AutorizacionMenor a JOIN FETCH a.usuario u LEFT JOIN FETCH u.perfil WHERE a.tokenHash = :hash")
    Optional<AutorizacionMenor> findByTokenHash(@Param("hash") String tokenHash);

    Optional<AutorizacionMenor> findByUsuarioId(Long idUsuario);

    boolean existsByUsuarioIdAndEstado(Long idUsuario, EstadoAutorizacion estado);
}
