package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndUsuarioIdNot(String nickname, Long idUsuario);
}
