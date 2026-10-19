package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.RespuestasEvaluacionDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.ResultadoEvaluacionDTO;
import co.edu.uniquindio.kinet.services.EvaluacionService;
import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "03. Evaluación de nivel", description = "Placement: cuestionario y nivel de habilidad")
@RestController
@RequestMapping("/api/evaluacion")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    @Operation(summary = "Preguntas activas del cuestionario")
    @GetMapping("/preguntas")
    public ResponseEntity<ResponseDTO<List<PreguntaDTO>>> preguntas() {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.preguntas()));
    }

    @Operation(summary = "Responder el cuestionario (una opción por pregunta). Se puede repetir cada 30 días")
    @PostMapping
    public ResponseEntity<ResponseDTO<ResultadoEvaluacionDTO>> responder(Authentication auth,
                                                                         @Valid @RequestBody RespuestasEvaluacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, evaluacionService.responder(auth.getName(), dto)));
    }

    @Operation(summary = "Mi evaluación vigente")
    @GetMapping
    public ResponseEntity<ResponseDTO<ResultadoEvaluacionDTO>> vigente(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.vigente(auth.getName())));
    }

    @Operation(summary = "Mis evaluaciones anteriores")
    @GetMapping("/historial")
    public ResponseEntity<ResponseDTO<PaginaDTO<ResultadoEvaluacionDTO>>> historial(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.historial(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }
}
