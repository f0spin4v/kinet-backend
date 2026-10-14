package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.EditarActividadDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.TipoActividadDTO;
import co.edu.uniquindio.kinet.dto.reto.CrearRetoDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.services.ActividadService;
import co.edu.uniquindio.kinet.services.RetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Solo para el rol ADMINISTRADOR: configura las reglas de gamificación del Proceso 2. */
@Tag(name = "8. Administración", description = "Reglas de gamificación: retos y XP por actividad")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RetoService retoService;
    private final ActividadService actividadService;

    @Operation(summary = "Todos los retos, activos e inactivos")
    @GetMapping("/retos")
    public ResponseEntity<ResponseDTO<List<RetoDTO>>> retos() {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.listarTodos()));
    }

    @Operation(summary = "Crear un reto")
    @PostMapping("/retos")
    public ResponseEntity<ResponseDTO<RetoDTO>> crearReto(@Valid @RequestBody CrearRetoDTO crearDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, retoService.crear(crearDTO)));
    }

    @Operation(summary = "Activar o desactivar un reto")
    @PatchMapping("/retos/{id}/estado")
    public ResponseEntity<ResponseDTO<RetoDTO>> cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.cambiarEstado(id, activo)));
    }

    @Operation(summary = "Cambiar el XP por minuto de una actividad")
    @PutMapping("/actividades/{codigo}")
    public ResponseEntity<ResponseDTO<TipoActividadDTO>> editarActividad(@PathVariable String codigo,
                                                                         @Valid @RequestBody EditarActividadDTO editarDTO) {
        return ResponseEntity.ok(new ResponseDTO<>(false, actividadService.editarXp(codigo, editarDTO)));
    }
}
