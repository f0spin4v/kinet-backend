package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Clan;
import co.edu.uniquindio.kinet.model.enums.EstadoClan;
import co.edu.uniquindio.kinet.model.enums.RolClan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClanRepository extends JpaRepository<Clan, Long> {
    List<Clan> findByEstadoOrderByNombreAsc(EstadoClan estado);

    /** Clan con su número de miembros y el gamertag del líder. */
    interface Resumen {
        Long getId();

        String getNombre();

        String getDescripcion();

        Integer getCapacidadMax();

        Long getMiembros();

        String getLider();

        EstadoClan getEstado();

        LocalDateTime getFechaCreacion();
    }

    /** Clanes de un estado, cada uno con miembros y líder, en una sola consulta. */
    @Query(value = """
            SELECT c.id AS id, c.nombre AS nombre, c.descripcion AS descripcion, c.capacidadMax AS capacidadMax,
                   (SELECT COUNT(m) FROM MembresiaClan m WHERE m.clan = c AND m.fechaSalida IS NULL) AS miembros,
                   (SELECT p.gamertag FROM MembresiaClan m JOIN PerfilJugador p ON p.id = m.usuario.id
                    WHERE m.clan = c AND m.fechaSalida IS NULL AND m.rolClan = :lider) AS lider,
                   c.estado AS estado, c.fechaCreacion AS fechaCreacion
            FROM Clan c WHERE c.estado = :estado ORDER BY c.nombre, c.id
            """,
            countQuery = "SELECT COUNT(c) FROM Clan c WHERE c.estado = :estado")
    Page<Resumen> resumenes(@Param("estado") EstadoClan estado, @Param("lider") RolClan lider, Pageable pagina);

    @Query("""
            SELECT c.id AS id, c.nombre AS nombre, c.descripcion AS descripcion, c.capacidadMax AS capacidadMax,
                   (SELECT COUNT(m) FROM MembresiaClan m WHERE m.clan = c AND m.fechaSalida IS NULL) AS miembros,
                   (SELECT p.gamertag FROM MembresiaClan m JOIN PerfilJugador p ON p.id = m.usuario.id
                    WHERE m.clan = c AND m.fechaSalida IS NULL AND m.rolClan = :lider) AS lider,
                   c.estado AS estado, c.fechaCreacion AS fechaCreacion
            FROM Clan c WHERE c.id = :idClan
            """)
    Optional<Resumen> resumen(@Param("idClan") Long idClan, @Param("lider") RolClan lider);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
