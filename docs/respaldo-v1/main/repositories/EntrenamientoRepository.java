package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {

    /** Historial del jugador, del más reciente al más antiguo. */
    List<Entrenamiento> findTop50ByUsuarioIdOrderByFechaInicioDesc(Long idUsuario);

    Optional<Entrenamiento> findByIdAndUsuarioId(Long id, Long idUsuario);

    /** Entrenamientos con un estado dado que empezaron dentro de un rango de fechas. */
    List<Entrenamiento> findByUsuarioIdAndEstadoAndFechaInicioBetween(
            Long idUsuario, EstadoEntrenamiento estado, LocalDateTime desde, LocalDateTime hasta);

    /** Hay cruce de horario si un entrenamiento empieza antes de que termine el otro y termina después de que empiece. */
    boolean existsByUsuarioIdAndEstadoAndFechaInicioLessThanAndFechaFinGreaterThan(
            Long idUsuario, EstadoEntrenamiento estado, LocalDateTime finNuevo, LocalDateTime inicioNuevo);
}
