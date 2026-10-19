package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Reto;
import co.edu.uniquindio.kinet.model.enums.EstadoReto;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.model.enums.Periodicidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RetoRepository extends JpaRepository<Reto, Long> {
    /**
     * Retos que el jugador puede ver ahora: activos, dentro de sus fechas, los de temporada solo con la temporada
     * activa, y los de todos los niveles o del nivel del jugador (idNivel = -1 si no tiene evaluación).
     */
    @Query(value = """
            SELECT r FROM Reto r LEFT JOIN r.temporada t
            WHERE r.estado = :activo AND r.fechaInicio <= :ahora AND (r.fechaFin IS NULL OR r.fechaFin > :ahora)
              AND (r.periodicidad <> :deTemporada OR t.estado = :temporadaActiva)
              AND (r.nivelHabilidad IS NULL OR r.nivelHabilidad.id = :idNivel)
            ORDER BY r.xpRecompensa, r.id
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reto r LEFT JOIN r.temporada t
            WHERE r.estado = :activo AND r.fechaInicio <= :ahora AND (r.fechaFin IS NULL OR r.fechaFin > :ahora)
              AND (r.periodicidad <> :deTemporada OR t.estado = :temporadaActiva)
              AND (r.nivelHabilidad IS NULL OR r.nivelHabilidad.id = :idNivel)
            """)
    Page<Reto> vigentesPara(@Param("ahora") LocalDateTime ahora, @Param("idNivel") Long idNivel,
                            @Param("activo") EstadoReto activo, @Param("deTemporada") Periodicidad deTemporada,
                            @Param("temporadaActiva") EstadoTemporada temporadaActiva, Pageable pagina);

    Page<Reto> findAllByOrderByIdAsc(Pageable pagina);
}
