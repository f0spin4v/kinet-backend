package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Temporada;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {
    Optional<Temporada> findFirstByEstado(EstadoTemporada estado);

    Page<Temporada> findAllByOrderByFechaInicioDescIdDesc(Pageable pagina);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
