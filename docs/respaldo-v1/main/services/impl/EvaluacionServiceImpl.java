package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.RespuestasEvaluacionDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.ResultadoEvaluacionDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.model.EvaluacionNivel;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import co.edu.uniquindio.kinet.repositories.EvaluacionNivelRepository;
import co.edu.uniquindio.kinet.services.Cuestionario;
import co.edu.uniquindio.kinet.services.EvaluacionService;
import co.edu.uniquindio.kinet.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluacionServiceImpl implements EvaluacionService {

    private final EvaluacionNivelRepository evaluacionRepository;
    private final UsuarioService usuarioService;

    @Override
    public List<PreguntaDTO> preguntas() {
        return Cuestionario.PREGUNTAS;
    }

    /** Califica las respuestas y guarda el nivel inicial. Solo se puede hacer una vez. */
    @Override
    @Transactional
    public ResultadoEvaluacionDTO registrar(String correo, RespuestasEvaluacionDTO respuestasDTO) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        if (evaluacionRepository.existsByUsuarioId(usuario.getId())) {
            throw new ConflictoException("Ya hiciste tu evaluación de nivel.");
        }
        // el puntaje se calcula aquí, nunca se recibe del navegador
        int puntaje = Cuestionario.calcularPuntaje(respuestasDTO.respuestas());
        EvaluacionNivel evaluacion = evaluacionRepository.save(EvaluacionNivel.builder()
                .usuario(usuario)
                .puntaje(puntaje)
                .nivelInicial(NivelInicial.segunPuntaje(puntaje))
                .build());
        return resultado(evaluacion);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoEvaluacionDTO consultar(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return evaluacionRepository.findByUsuarioId(usuario.getId())
                .map(this::resultado)
                .orElseThrow(() -> new NoEncontradoException("Aún no has hecho tu evaluación de nivel."));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NivelInicial> nivelDe(Long idUsuario) {
        return evaluacionRepository.findByUsuarioId(idUsuario).map(EvaluacionNivel::getNivelInicial);
    }

    private ResultadoEvaluacionDTO resultado(EvaluacionNivel evaluacion) {
        return new ResultadoEvaluacionDTO(evaluacion.getPuntaje(), evaluacion.getNivelInicial().name(),
                evaluacion.getFecha());
    }
}
