package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.reto.AporteManualDTO;
import co.edu.uniquindio.kinet.dto.reto.ParticipacionDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.services.RetoService;
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

@Tag(name = "07. Retos", description = "Retos y misiones: individuales, de clan y de temporada")
@RestController
@RequestMapping("/api/retos")
@RequiredArgsConstructor
public class RetoController {

    private final RetoService retoService;

    @Operation(summary = "Retos disponibles para mí, con mi avance del periodo actual")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<RetoDTO>>> disponibles(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.disponibles(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Mis participaciones")
    @GetMapping("/mios")
    public ResponseEntity<ResponseDTO<PaginaDTO<ParticipacionDTO>>> misRetos(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.misRetos(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Detalle de un reto")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<RetoDTO>> detalle(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.detalle(auth.getName(), id)));
    }

    @Operation(summary = "Unirme a un reto en el periodo actual")
    @PostMapping("/{id}/participaciones")
    public ResponseEntity<ResponseDTO<ParticipacionDTO>> unirse(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, retoService.unirse(auth.getName(), id)));
    }

    @Operation(summary = "Registrar un aporte manual (retos de REPETICIONES)")
    @PostMapping("/participaciones/{id}/aportes")
    public ResponseEntity<ResponseDTO<ParticipacionDTO>> aportar(Authentication auth, @PathVariable Long id,
                                                                 @Valid @RequestBody AporteManualDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, retoService.aportarManual(auth.getName(), id, dto)));
    }

    @Operation(summary = "Abandonar una participación en curso")
    @PostMapping("/participaciones/{id}/abandono")
    public ResponseEntity<ResponseDTO<ParticipacionDTO>> abandonar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.abandonar(auth.getName(), id)));
    }
}
