package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.LogroUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogroUsuarioRepository extends JpaRepository<LogroUsuario, LogroUsuario.Id> {
    boolean existsByLogroId(Long idLogro);
}
