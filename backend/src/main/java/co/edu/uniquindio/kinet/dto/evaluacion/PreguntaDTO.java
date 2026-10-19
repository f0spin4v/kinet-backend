package co.edu.uniquindio.kinet.dto.evaluacion;

import co.edu.uniquindio.kinet.model.PreguntaEvaluacion;
import java.util.List;

/** Pregunta tal como la ve el jugador. No incluye a qué nivel vota cada opción ni su peso. */
public record PreguntaDTO(Long id, Integer orden, String enunciado, List<OpcionDTO> opciones) {

    public record OpcionDTO(Long id, String titulo, String descripcion) {
    }

    public static PreguntaDTO de(PreguntaEvaluacion pregunta) {
        return new PreguntaDTO(pregunta.getId(), pregunta.getOrden(), pregunta.getEnunciado(),
                pregunta.getOpciones().stream()
                        .map(o -> new OpcionDTO(o.getId(), o.getTitulo(), o.getDescripcion()))
                        .toList());
    }
}
