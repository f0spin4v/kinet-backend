package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.config.CacheConfig;
import co.edu.uniquindio.kinet.model.NivelProgresion;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NivelProgresionRepository extends JpaRepository<NivelProgresion, Long> {
    /** En caché: se lee en cada movimiento de XP y solo cambia cuando el administrador edita un nivel. */
    @Cacheable(CacheConfig.NIVELES_PROGRESION)
    List<NivelProgresion> findAllByOrderByNumeroNivelAsc();

    @Override
    @CacheEvict(value = CacheConfig.NIVELES_PROGRESION, allEntries = true)
    <S extends NivelProgresion> S save(S nivel);

    Optional<NivelProgresion> findByNumeroNivel(Integer numeroNivel);

}
