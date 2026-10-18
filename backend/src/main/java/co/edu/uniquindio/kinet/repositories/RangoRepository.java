package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.Rango;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RangoRepository extends JpaRepository<Rango, Long> {
    List<Rango> findAllByOrderByOrdenAsc();

    /** Rango que corresponde a unos puntos: el de mayor puntos_minimo alcanzado. */
    Optional<Rango> findFirstByPuntosMinimoLessThanEqualOrderByPuntosMinimoDesc(Integer puntos);

    Optional<Rango> findFirstByLigaIdOrderByOrdenAsc(Long idLiga);

    Optional<Rango> findFirstByLigaIdOrderByOrdenDesc(Long idLiga);
}
