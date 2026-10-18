package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.reto.CambiarEstadoRetoDTO;
import co.edu.uniquindio.kinet.dto.reto.GuardarRetoDTO;
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

@Tag(name = "21. Admin: retos", description = "Catálogo de retos: borrador, publicación y cierre")
@RestController
@RequestMapping("/api/admin/retos")
@RequiredArgsConstructor
public class AdminRetoController {

    private final RetoService retoService;

    @Operation(summary = "Todos los retos")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<RetoDTO>>> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.listarTodos(PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Crear un reto (queda en BORRADOR)")
    @PostMapping
    public ResponseEntity<ResponseDTO<RetoDTO>> crear(@Valid @RequestBody GuardarRetoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, retoService.crear(dto)));
    }

    @Operation(summary = "Editar un reto en borrador")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<RetoDTO>> editar(@PathVariable Long id, @Valid @RequestBody GuardarRetoDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.editar(id, dto)));
    }

    @Operation(summary = "Publicar (ACTIVO) o finalizar (FINALIZADO) un reto")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ResponseDTO<RetoDTO>> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRetoDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, retoService.cambiarEstado(id, dto.estado())));
    }
}
