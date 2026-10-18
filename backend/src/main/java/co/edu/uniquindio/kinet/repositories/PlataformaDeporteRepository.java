package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.config.CacheConfig;
import co.edu.uniquindio.kinet.model.PlataformaDeporte;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlataformaDeporteRepository extends JpaRepository<PlataformaDeporte, Long> {

    /** Las apps aceptadas (activa = true). En caché: se lee en cada enlace que sube un jugador y casi nunca cambia. */
    @Cacheable(CacheConfig.PLATAFORMAS_DEPORTE)
    List<PlataformaDeporte> findByActivaOrderByIdAsc(Boolean activa);

    @Override
    @CacheEvict(value = CacheConfig.PLATAFORMAS_DEPORTE, allEntries = true)
    <S extends PlataformaDeporte> S save(S plataforma);
}
