package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.RespuestasEvaluacionDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.ResultadoEvaluacionDTO;
import co.edu.uniquindio.kinet.services.EvaluacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. Evaluación de nivel", description = "Proceso 1: Placement Match (UI-001)")
@RestController
@RequestMapping("/api/evaluacion")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    @Operation(summary = "Ver las 5 preguntas")
    @GetMapping("/preguntas")
    public ResponseEntity<ResponseDTO<List<PreguntaDTO>>> preguntas() {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.preguntas()));
    }

    @Operation(summary = "Ver mi resultado (404 si aún no la hice)")
    @GetMapping
    public ResponseEntity<ResponseDTO<ResultadoEvaluacionDTO>> consultar(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.consultar(autenticacion.getName())));
    }

    @Operation(summary = "Enviar mis respuestas y recibir mi nivel inicial")
    @PostMapping
    public ResponseEntity<ResponseDTO<ResultadoEvaluacionDTO>> registrar(Authentication autenticacion,
                                                                         @Valid @RequestBody RespuestasEvaluacionDTO respuestasDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, evaluacionService.registrar(autenticacion.getName(), respuestasDTO)));
    }
}
