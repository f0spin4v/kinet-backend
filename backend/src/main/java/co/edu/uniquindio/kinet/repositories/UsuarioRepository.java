package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** El usuario con su rol y su perfil en una sola consulta (el perfil es el lado inverso de un 1:1). */
    @EntityGraph(attributePaths = {"rol", "perfil"})
    Optional<Usuario> findByCorreo(String correo);

    @Override
    @EntityGraph(attributePaths = {"rol", "perfil"})
    Optional<Usuario> findById(Long id);

    /** Varios usuarios con su rol y su perfil en una sola consulta. */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.perfil WHERE u.id IN :ids")
    List<Usuario> conRolYPerfil(@Param("ids") Collection<Long> ids);

    @Query("SELECT u.id FROM Usuario u WHERE u.correo = :correo")
    Optional<Long> idPorCorreo(@Param("correo") String correo);

    boolean existsByCorreo(String correo);

    /** Búsqueda para el administrador por parte del correo, con rol y perfil. */
    @Query(value = """
            SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.perfil
            WHERE u.correo LIKE %:correo% ESCAPE '\\' ORDER BY u.id
            """,
            countQuery = "SELECT COUNT(u) FROM Usuario u WHERE u.correo LIKE %:correo% ESCAPE '\\'")
    Page<Usuario> buscar(@Param("correo") String correo, Pageable pagina);

    @Query(value = """
            SELECT u FROM Usuario u JOIN FETCH u.rol LEFT JOIN FETCH u.perfil
            WHERE u.correo LIKE %:correo% ESCAPE '\\' AND u.estado = :estado ORDER BY u.id
            """,
            countQuery = "SELECT COUNT(u) FROM Usuario u WHERE u.correo LIKE %:correo% ESCAPE '\\' AND u.estado = :estado")
    Page<Usuario> buscar(@Param("correo") String correo, @Param("estado") EstadoUsuario estado, Pageable pagina);

    @Query("SELECT CAST(u.estado AS string) AS clave, COUNT(u) AS total FROM Usuario u GROUP BY u.estado")
    List<Conteo> contarPorEstado();

    @Query("SELECT u.rol.nombre AS clave, COUNT(u) AS total FROM Usuario u GROUP BY u.rol.nombre")
    List<Conteo> contarPorRol();
}
