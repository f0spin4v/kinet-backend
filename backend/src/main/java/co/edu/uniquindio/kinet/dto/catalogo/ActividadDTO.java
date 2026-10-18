package co.edu.uniquindio.kinet.dto.catalogo;

import co.edu.uniquindio.kinet.model.TipoActividad;

import java.util.List;

/** Actividad con las reglas de puntuación vigentes (una por intensidad). */
public record ActividadDTO(Long id, String nombre, String estado, List<ReglaDTO> reglasVigentes) {

    public static ActividadDTO de(TipoActividad actividad, List<ReglaDTO> reglas) {
        return new ActividadDTO(actividad.getId(), actividad.getNombre(), actividad.getEstado().name(), reglas);
    }
}
