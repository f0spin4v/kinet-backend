package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.competencia.CompetenciaDTO;
import co.edu.uniquindio.kinet.dto.competencia.CrearCompetenciaDTO;
import co.edu.uniquindio.kinet.services.CompetenciaService;
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

@Tag(name = "06. Competencias", description = "Duelos, competencias grupales y torneos por invitación (amistosos, dan XP)")
@RestController
@RequestMapping("/api/competencias")
@RequiredArgsConstructor
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    @Operation(summary = "Crear una competencia e invitar jugadores")
    @PostMapping
    public ResponseEntity<ResponseDTO<CompetenciaDTO>> crear(Authentication auth, @Valid @RequestBody CrearCompetenciaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, competenciaService.crear(auth.getName(), dto)));
    }

    @Operation(summary = "Mis competencias (aceptadas o creadas)")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<CompetenciaDTO>>> mias(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.misCompetencias(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Invitaciones pendientes por responder")
    @GetMapping("/invitaciones")
    public ResponseEntity<ResponseDTO<PaginaDTO<CompetenciaDTO>>> invitaciones(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.invitaciones(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Detalle de una competencia, con el puntaje en vivo")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<CompetenciaDTO>> detalle(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.detalle(auth.getName(), id)));
    }

    @Operation(summary = "Aceptar la invitación (un duelo empieza de inmediato)")
    @PostMapping("/{id}/aceptacion")
    public ResponseEntity<ResponseDTO<CompetenciaDTO>> aceptar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.aceptar(auth.getName(), id)));
    }

    @Operation(summary = "Rechazar la invitación (un duelo rechazado se cancela)")
    @PostMapping("/{id}/rechazo")
    public ResponseEntity<ResponseDTO<CompetenciaDTO>> rechazar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.rechazar(auth.getName(), id)));
    }

    @Operation(summary = "Cancelar una competencia que creé y no ha empezado")
    @PostMapping("/{id}/cancelacion")
    public ResponseEntity<ResponseDTO<CompetenciaDTO>> cancelar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, competenciaService.cancelar(auth.getName(), id)));
    }
}
