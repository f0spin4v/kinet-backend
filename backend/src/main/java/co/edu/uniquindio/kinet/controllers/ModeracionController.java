package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.*;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.dto.temporada.DescalificarDTO;
import co.edu.uniquindio.kinet.services.CierreTemporadaService;
import co.edu.uniquindio.kinet.services.ClanService;
import co.edu.uniquindio.kinet.services.EntrenamientoService;
import co.edu.uniquindio.kinet.services.EvidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "25. Moderación", description = "Entrenamientos pendientes, evidencias y cierre de clanes")
@RestController
@RequestMapping("/api/moderacion")
@RequiredArgsConstructor
public class ModeracionController {

    private final EntrenamientoService entrenamientoService;
    private final EvidenciaService evidenciaService;
    private final ClanService clanService;
    private final CierreTemporadaService cierreTemporadaService;

    @Operation(summary = "Entrenamientos pendientes de revisión (del más antiguo al más reciente)")
    @GetMapping("/entrenamientos/pendientes")
    public ResponseEntity<ResponseDTO<PaginaDTO<EntrenamientoPendienteDTO>>> pendientes(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.pendientes(PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Aprobar o rechazar un entrenamiento pendiente")
    @PostMapping("/entrenamientos/{id}/revision")
    public ResponseEntity<ResponseDTO<ResultadoEntrenamientoDTO>> revisar(@PathVariable Long id,
                                                                          @Valid @RequestBody RevisionEntrenamientoDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.revisar(id, dto)));
    }

    @Operation(summary = "Evidencias subidas (por defecto, las que nadie ha revisado)")
    @GetMapping("/evidencias")
    public ResponseEntity<ResponseDTO<PaginaDTO<EvidenciaDTO>>> evidencias(
            @RequestParam(required = false, defaultValue = "SIN_REVISAR") EstadoEvidencia estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false,
                evidenciaService.listar(estado, PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Ver la imagen de una evidencia")
    @GetMapping("/evidencias/{id}/archivo")
    public ResponseEntity<byte[]> archivo(@PathVariable Long id) {
        return EntrenamientoController.imagen(evidenciaService.archivo(id));
    }

    @Operation(summary = "Aceptar o rechazar una evidencia (rechazar revierte su bonus de XP)")
    @PostMapping("/evidencias/{id}/revision")
    public ResponseEntity<ResponseDTO<EvidenciaDTO>> revisarEvidencia(@PathVariable Long id,
                                                                     @Valid @RequestBody RevisionEvidenciaDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, evidenciaService.revisar(id, dto)));
    }

    @Operation(summary = "Cerrar un clan")
    @PostMapping("/clanes/{id}/cierre")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cerrarClan(Authentication auth, @PathVariable Long id) {
        clanService.cerrar(auth.getName(), id, true);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El clan quedó cerrado.")));
    }

    @Operation(summary = "Descalificar a un jugador de la temporada activa")
    @PostMapping("/temporadas/activa/participantes/{idUsuario}/descalificacion")
    public ResponseEntity<ResponseDTO<MensajeDTO>> descalificar(@PathVariable Long idUsuario,
                                                               @Valid @RequestBody DescalificarDTO dto) {
        cierreTemporadaService.descalificar(idUsuario, dto.motivo());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El jugador quedó descalificado.")));
    }
}
