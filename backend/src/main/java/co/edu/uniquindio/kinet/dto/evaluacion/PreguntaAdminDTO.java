package co.edu.uniquindio.kinet.dto.evaluacion;

import co.edu.uniquindio.kinet.model.PreguntaEvaluacion;
import java.util.List;

/** Pregunta con los votos de cada opción, para administración. */
public record PreguntaAdminDTO(Long id, Integer orden, String enunciado, Boolean activa, List<OpcionAdminDTO> opciones) {

    public record OpcionAdminDTO(Long id, String titulo, String descripcion, String nivelHabilidad, Integer puntaje) {
    }

    public static PreguntaAdminDTO de(PreguntaEvaluacion pregunta) {
        return new PreguntaAdminDTO(pregunta.getId(), pregunta.getOrden(), pregunta.getEnunciado(), pregunta.getActiva(),
                pregunta.getOpciones().stream()
                        .map(o -> new OpcionAdminDTO(o.getId(), o.getTitulo(), o.getDescripcion(),
                                o.getNivelHabilidad().getNombre(), o.getPuntaje()))
                        .toList());
    }
}
