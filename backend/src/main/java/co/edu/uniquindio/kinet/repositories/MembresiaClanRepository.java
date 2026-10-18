package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.dto.clan.MiembroDTO;
import co.edu.uniquindio.kinet.model.MembresiaClan;
import co.edu.uniquindio.kinet.model.enums.RolClan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaClanRepository extends JpaRepository<MembresiaClan, Long> {
    /** Membresía vigente del jugador (como máximo una). */
    Optional<MembresiaClan> findByUsuarioIdAndFechaSalidaIsNull(Long idUsuario);

    /** Miembros vigentes con su usuario (para avisarles), el más antiguo primero. */
    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "usuario.perfil"})
    List<MembresiaClan> findByClanIdAndFechaSalidaIsNullOrderByFechaIngresoAsc(Long idClan);

    /** Miembros vigentes ya como DTO, con su gamertag. */
    @Query("""
            SELECT new co.edu.uniquindio.kinet.dto.clan.MiembroDTO(m.usuario.id, p.gamertag, CAST(m.rolClan AS string),
                   m.fechaIngreso)
            FROM MembresiaClan m JOIN PerfilJugador p ON p.id = m.usuario.id
            WHERE m.clan.id = :idClan AND m.fechaSalida IS NULL ORDER BY m.fechaIngreso
            """)
    List<MiembroDTO> miembros(@Param("idClan") Long idClan);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "usuario.perfil"})
    Optional<MembresiaClan> findFirstByClanIdAndRolClanAndFechaSalidaIsNull(Long idClan, RolClan rolClan);

    /** El miembro más antiguo, sin contar una membresía (la del líder que sale). */
    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "usuario.perfil"})
    Optional<MembresiaClan> findFirstByClanIdAndFechaSalidaIsNullAndIdNotOrderByFechaIngresoAsc(Long idClan,
                                                                                               Long idMembresia);

    Optional<MembresiaClan> findByUsuarioIdAndClanIdAndFechaSalidaIsNull(Long idUsuario, Long idClan);

    boolean existsByUsuarioIdAndFechaSalidaIsNull(Long idUsuario);

    /** Cierra todas las membresías vigentes del clan. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE MembresiaClan m SET m.fechaSalida = :ahora WHERE m.clan.id = :idClan AND m.fechaSalida IS NULL")
    int terminarTodas(@Param("idClan") Long idClan, @Param("ahora") LocalDateTime ahora);

    long countByClanIdAndFechaSalidaIsNull(Long idClan);
}
