package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.TipoActividadDTO;
import co.edu.uniquindio.kinet.services.ActividadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4. Entrenamientos", description = "Proceso 2: registro y validación de entrenamientos")
@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;

    @Operation(summary = "Tipos de actividad y su XP por minuto")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<TipoActividadDTO>>> listar() {
        return ResponseEntity.ok(new ResponseDTO<>(false, actividadService.listar()));
    }
}
