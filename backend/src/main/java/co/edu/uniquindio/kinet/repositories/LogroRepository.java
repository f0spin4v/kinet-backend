package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Logro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogroRepository extends JpaRepository<Logro, Long> {
    List<Logro> findAllByOrderByIdAsc();

    /** Logro con la fecha en que el jugador lo obtuvo (null si no lo tiene). */
    interface ConFecha {
        Logro getLogro();

        LocalDateTime getFechaObtenido();
    }

    /** Catálogo completo con lo obtenido por el jugador, en una sola consulta. */
    @Query("""
            SELECT l AS logro, lu.fechaObtenido AS fechaObtenido FROM Logro l
            LEFT JOIN LogroUsuario lu ON lu.logro = l AND lu.usuario.id = :idUsuario
            ORDER BY l.id
            """)
    List<ConFecha> catalogoDe(@Param("idUsuario") Long idUsuario);

    /** Logros que el jugador todavía no tiene. */
    @Query("""
            SELECT l FROM Logro l
            WHERE NOT EXISTS (SELECT 1 FROM LogroUsuario lu WHERE lu.logro = l AND lu.usuario.id = :idUsuario)
            ORDER BY l.id
            """)
    List<Logro> pendientesDe(@Param("idUsuario") Long idUsuario);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
