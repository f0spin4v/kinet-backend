package co.edu.uniquindio.kinet.dto.evaluacion;

import java.time.LocalDateTime;

public record ResultadoEvaluacionDTO(int puntaje, String nivelInicial, LocalDateTime fecha) {
}
