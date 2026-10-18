package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.temporada.GuardarTemporadaDTO;
import co.edu.uniquindio.kinet.dto.temporada.TemporadaDTO;
import co.edu.uniquindio.kinet.services.CierreTemporadaService;
import co.edu.uniquindio.kinet.services.TemporadaService;
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

@Tag(name = "22. Admin: temporadas", description = "Programar, activar y cerrar temporadas")
@RestController
@RequestMapping("/api/admin/temporadas")
@RequiredArgsConstructor
public class AdminTemporadaController {

    private final TemporadaService temporadaService;
    private final CierreTemporadaService cierreTemporadaService;

    @Operation(summary = "Todas las temporadas")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<TemporadaDTO>>> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.listar(PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Programar una temporada")
    @PostMapping
    public ResponseEntity<ResponseDTO<TemporadaDTO>> crear(@Valid @RequestBody GuardarTemporadaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, temporadaService.crear(dto)));
    }

    @Operation(summary = "Editar una temporada programada")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<TemporadaDTO>> editar(@PathVariable Long id, @Valid @RequestBody GuardarTemporadaDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.editar(id, dto)));
    }

    @Operation(summary = "Activar una temporada programada")
    @PostMapping("/{id}/activacion")
    public ResponseEntity<ResponseDTO<TemporadaDTO>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.activar(id)));
    }

    @Operation(summary = "Cerrar la temporada activa (foto final de puntos y posiciones)")
    @PostMapping("/{id}/cierre")
    public ResponseEntity<ResponseDTO<TemporadaDTO>> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, cierreTemporadaService.cerrar(id)));
    }
}
