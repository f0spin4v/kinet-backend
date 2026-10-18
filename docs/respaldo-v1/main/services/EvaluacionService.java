package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.RespuestasEvaluacionDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.ResultadoEvaluacionDTO;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;

import java.util.List;
import java.util.Optional;

/** Proceso 1: evaluación de nivel inicial (Placement Match). */
public interface EvaluacionService {

    List<PreguntaDTO> preguntas();

    ResultadoEvaluacionDTO registrar(String correo, RespuestasEvaluacionDTO respuestasDTO);

    ResultadoEvaluacionDTO consultar(String correo);

    Optional<NivelInicial> nivelDe(Long idUsuario);
}
