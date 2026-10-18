package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.HistorialRango;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialRangoRepository extends JpaRepository<HistorialRango, Long> {
    Page<HistorialRango> findByParticipacionUsuarioIdAndParticipacionTemporadaIdOrderByFechaCambioAscIdAsc(
            Long idUsuario, Long idTemporada, Pageable pagina);
}
