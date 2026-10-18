package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.Liga;
import java.util.List;

public record LigaDTO(Long id, String nombre, Integer orden, Integer cuposAscenso, Integer cuposDescenso,
                      List<RangoDTO> rangos) {
    public static LigaDTO de(Liga liga, List<RangoDTO> rangos) {
        return new LigaDTO(liga.getId(), liga.getNombre(), liga.getOrden(), liga.getCuposAscenso(),
                liga.getCuposDescenso(), rangos);
    }
}
