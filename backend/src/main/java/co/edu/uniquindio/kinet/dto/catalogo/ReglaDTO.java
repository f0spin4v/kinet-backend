package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.ReglaPuntuacion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReglaDTO(
        Long id,
        Long idTipoActividad,
        String actividad,
        String intensidad,
        BigDecimal xpPorMinuto,
        Integer duracionMinimaMin,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
) {
    public static ReglaDTO de(ReglaPuntuacion regla) {
        return new ReglaDTO(regla.getId(), regla.getTipoActividad().getId(), regla.getTipoActividad().getNombre(),
                regla.getIntensidad().name(), regla.getXpPorMinuto(), regla.getDuracionMinimaMin(),
                regla.getVigenteDesde(), regla.getVigenteHasta());
    }
}
