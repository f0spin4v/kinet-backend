package co.edu.uniquindio.kinet.dto.evaluacion;

import java.util.List;

/** Pregunta tal como la ve el jugador. No incluye cuántos puntos vale cada opción. */
public record PreguntaDTO(int numero, String texto, List<OpcionDTO> opciones) {

    public record OpcionDTO(String id, String titulo, String descripcion) {
    }
}
