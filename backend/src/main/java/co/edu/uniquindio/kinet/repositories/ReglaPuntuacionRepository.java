package co.edu.uniquindio.kinet.repositories;

import co.edu.uniquindio.kinet.model.ReglaPuntuacion;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReglaPuntuacionRepository extends JpaRepository<ReglaPuntuacion, Long> {

    /** Regla que aplicaba en una fecha. vigente_hasta es el último día de vigencia (incluido). */
    @Query("""
            SELECT r FROM ReglaPuntuacion r
            WHERE r.tipoActividad.id = :idActividad AND r.intensidad = :intensidad
              AND r.vigenteDesde <= :fecha AND (r.vigenteHasta IS NULL OR r.vigenteHasta >= :fecha)
            """)
    Optional<ReglaPuntuacion> vigenteEn(@Param("idActividad") Long idActividad,
                                        @Param("intensidad") Intensidad intensidad,
                                        @Param("fecha") LocalDate fecha);

    /** Todas las reglas vigentes en una fecha, con su actividad. */
    @Query("""
            SELECT r FROM ReglaPuntuacion r JOIN FETCH r.tipoActividad
            WHERE r.vigenteDesde <= :fecha AND (r.vigenteHasta IS NULL OR r.vigenteHasta >= :fecha)
            """)
    List<ReglaPuntuacion> vigentesEn(@Param("fecha") LocalDate fecha);

    Optional<ReglaPuntuacion> findByTipoActividadIdAndIntensidadAndVigenteHastaIsNull(Long idActividad,
                                                                                     Intensidad intensidad);

    List<ReglaPuntuacion> findByVigenteHastaIsNullOrderByTipoActividadNombreAscIntensidadAsc();

    List<ReglaPuntuacion> findByTipoActividadIdOrderByIntensidadAscVigenteDesdeDesc(Long idActividad);
}
