package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.reto.ParticipacionDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.services.RetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "5. Retos", description = "Proceso 2: retos y misiones (UI-004, UI-005)")
@RestController
@RequestMapping("/api/retos")
@RequiredArgsConstructor
public class RetoController {

    private final RetoService retoService;

    @Operation(summary = "Retos disponibles para mi nivel, con mi avance si ya me uní")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<RetoDTO>>> disponibles(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.listarDisponibles(autenticacion.getName())));
    }

    @Operation(summary = "Mis participaciones (en curso, completadas y vencidas)")
    @GetMapping("/mios")
    public ResponseEntity<ResponseDTO<List<ParticipacionDTO>>> misRetos(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.misRetos(autenticacion.getName())));
    }

    @Operation(summary = "Detalle de un reto")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<RetoDTO>> detalle(Authentication autenticacion, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.detalle(autenticacion.getName(), id)));
    }

    @Operation(summary = "Unirme a un reto")
    @PostMapping("/{id}/participaciones")
    public ResponseEntity<ResponseDTO<ParticipacionDTO>> unirse(Authentication autenticacion, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, retoService.unirse(autenticacion.getName(), id)));
    }
}
