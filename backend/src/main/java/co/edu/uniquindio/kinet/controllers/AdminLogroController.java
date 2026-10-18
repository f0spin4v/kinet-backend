package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.logro.GuardarLogroDTO;
import co.edu.uniquindio.kinet.dto.logro.LogroDTO;
import co.edu.uniquindio.kinet.services.LogroService;
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

@Tag(name = "23. Admin: logros", description = "Catálogo de logros")
@RestController
@RequestMapping("/api/admin/logros")
@RequiredArgsConstructor
public class AdminLogroController {

    private final LogroService logroService;

    @Operation(summary = "Crear un logro")
    @PostMapping
    public ResponseEntity<ResponseDTO<LogroDTO>> crear(@Valid @RequestBody GuardarLogroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, logroService.crear(dto)));
    }

    @Operation(summary = "Editar un logro")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<LogroDTO>> editar(@PathVariable Long id, @Valid @RequestBody GuardarLogroDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, logroService.editar(id, dto)));
    }

    @Operation(summary = "Borrar un logro que nadie ha obtenido")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<MensajeDTO>> eliminar(@PathVariable Long id) {
        logroService.eliminar(id);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El logro fue borrado.")));
    }
}
