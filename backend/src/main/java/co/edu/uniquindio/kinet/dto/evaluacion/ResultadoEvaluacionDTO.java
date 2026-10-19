package co.edu.uniquindio.kinet.dto.evaluacion;

import java.time.LocalDateTime;
import java.util.Map;

/** votos: peso acumulado por nivel. El nivel asignado es el más votado (empate: el de menor orden). */
public record ResultadoEvaluacionDTO(
        Long id,
        String nivelHabilidad,
        Map<String, Integer> votos,
        String versionAlgoritmo,
        LocalDateTime fecha,
        LocalDateTime puedeRepetirDesde
) {
}
