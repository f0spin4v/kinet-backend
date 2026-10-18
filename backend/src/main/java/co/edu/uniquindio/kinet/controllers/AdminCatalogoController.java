package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.catalogo.*;
import co.edu.uniquindio.kinet.dto.evaluacion.CambiarEstadoPreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.GuardarPreguntaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.PreguntaAdminDTO;
import co.edu.uniquindio.kinet.services.CatalogoService;
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

import java.util.List;

@Tag(name = "20. Admin: catálogos", description = "Actividades, reglas de XP, niveles y cuestionario")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private final CatalogoService catalogoService;
    private final EvaluacionService evaluacionService;

    @Operation(summary = "Todas las actividades, también las inactivas")
    @GetMapping("/actividades")
    public ResponseEntity<ResponseDTO<List<ActividadDTO>>> actividades() {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.actividades(true)));
    }

    @Operation(summary = "Activar o desactivar una actividad")
    @PatchMapping("/actividades/{id}/estado")
    public ResponseEntity<ResponseDTO<ActividadDTO>> estadoActividad(@PathVariable Long id,
                                                                     @Valid @RequestBody CambiarEstadoActividadDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.cambiarEstadoActividad(id, dto.estado())));
    }

    @Operation(summary = "Crear una actividad")
    @PostMapping("/actividades")
    public ResponseEntity<ResponseDTO<ActividadDTO>> crearActividad(@Valid @RequestBody GuardarActividadDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, catalogoService.crearActividad(dto)));
    }

    @Operation(summary = "Editar una actividad")
    @PutMapping("/actividades/{id}")
    public ResponseEntity<ResponseDTO<ActividadDTO>> editarActividad(@PathVariable Long id,
                                                                     @Valid @RequestBody GuardarActividadDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.editarActividad(id, dto)));
    }

    @Operation(summary = "Reglas de puntuación: vigentes, o el historial de una actividad")
    @GetMapping("/reglas-puntuacion")
    public ResponseEntity<ResponseDTO<List<ReglaDTO>>> reglas(@RequestParam(required = false) Long idTipoActividad) {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.reglas(idTipoActividad)));
    }

    @Operation(summary = "Nueva regla de puntuación (cierra la anterior)")
    @PostMapping("/reglas-puntuacion")
    public ResponseEntity<ResponseDTO<ReglaDTO>> crearRegla(@Valid @RequestBody CrearReglaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, catalogoService.crearRegla(dto)));
    }

    @Operation(summary = "Cambiar el XP requerido de un nivel")
    @PutMapping("/niveles-progresion/{numero}")
    public ResponseEntity<ResponseDTO<NivelProgresionDTO>> editarNivel(@PathVariable Integer numero,
                                                                       @Valid @RequestBody EditarNivelProgresionDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.editarNivelProgresion(numero, dto.xpRequerido())));
    }

    @Operation(summary = "Preguntas del cuestionario con sus votos")
    @GetMapping("/preguntas")
    public ResponseEntity<ResponseDTO<List<PreguntaAdminDTO>>> preguntas() {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.preguntasAdmin()));
    }

    @Operation(summary = "Crear una pregunta con sus opciones")
    @PostMapping("/preguntas")
    public ResponseEntity<ResponseDTO<PreguntaAdminDTO>> crearPregunta(@Valid @RequestBody GuardarPreguntaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, evaluacionService.crearPregunta(dto)));
    }

    @Operation(summary = "Editar una pregunta (las opciones solo si nadie la ha respondido)")
    @PutMapping("/preguntas/{id}")
    public ResponseEntity<ResponseDTO<PreguntaAdminDTO>> editarPregunta(@PathVariable Long id,
                                                                        @Valid @RequestBody GuardarPreguntaDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.editarPregunta(id, dto)));
    }

    @Operation(summary = "Activar o desactivar una pregunta")
    @PatchMapping("/preguntas/{id}/estado")
    public ResponseEntity<ResponseDTO<PreguntaAdminDTO>> estadoPregunta(@PathVariable Long id,
                                                                        @Valid @RequestBody CambiarEstadoPreguntaDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evaluacionService.cambiarEstadoPregunta(id, dto.activa())));
    }
}
