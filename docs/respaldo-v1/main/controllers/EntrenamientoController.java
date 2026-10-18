package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.RegistrarEntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.ResultadoEntrenamientoDTO;
import co.edu.uniquindio.kinet.services.EntrenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4. Entrenamientos", description = "Proceso 2: registro y validación de entrenamientos")
@RestController
@RequestMapping("/api/entrenamientos")
@RequiredArgsConstructor
public class EntrenamientoController {

    private final EntrenamientoService entrenamientoService;

    @Operation(summary = "Registrar un entrenamiento (UI-006)",
            description = "Responde 201 siempre que los datos estén completos. Si el anti-trampa lo rechaza, "
                    + "entrenamiento.estado = RECHAZADO y motivoRechazo explica la causa.")
    @PostMapping
    public ResponseEntity<ResponseDTO<ResultadoEntrenamientoDTO>> registrar(Authentication autenticacion,
                                                                            @Valid @RequestBody RegistrarEntrenamientoDTO registrarDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, entrenamientoService.registrar(autenticacion.getName(), registrarDTO)));
    }

    @Operation(summary = "Mi historial (últimos 50)")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<EntrenamientoDTO>>> historial(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.historial(autenticacion.getName())));
    }

    @Operation(summary = "Ver un entrenamiento mío")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<EntrenamientoDTO>> obtener(Authentication autenticacion, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.obtener(autenticacion.getName(), id)));
    }
}
