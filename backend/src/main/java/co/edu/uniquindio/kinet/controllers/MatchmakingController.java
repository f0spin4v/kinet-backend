package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.matchmaking.EnfrentamientoDTO;
import co.edu.uniquindio.kinet.dto.matchmaking.SolicitudDTO;
import co.edu.uniquindio.kinet.services.MatchmakingService;
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

@Tag(name = "10. Matchmaking", description = "Pedir rival y ver enfrentamientos de 24 horas")
@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @Operation(summary = "Pedir un enfrentamiento (la primera vez en la temporada es la colocación)")
    @PostMapping("/solicitudes")
    public ResponseEntity<ResponseDTO<SolicitudDTO>> solicitar(Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, matchmakingService.solicitar(auth.getName())));
    }

    @Operation(summary = "Salir de la cola")
    @DeleteMapping("/solicitudes/actual")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cancelar(Authentication auth) {
        matchmakingService.cancelar(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Saliste de la cola de matchmaking.")));
    }

    @Operation(summary = "Mis enfrentamientos")
    @GetMapping("/enfrentamientos")
    public ResponseEntity<ResponseDTO<PaginaDTO<EnfrentamientoDTO>>> mios(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, matchmakingService.misEnfrentamientos(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Detalle de un enfrentamiento mío (puntaje en vivo si sigue en curso)")
    @GetMapping("/enfrentamientos/{id}")
    public ResponseEntity<ResponseDTO<EnfrentamientoDTO>> detalle(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, matchmakingService.detalle(auth.getName(), id)));
    }
}
