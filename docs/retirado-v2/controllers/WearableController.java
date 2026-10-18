package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.wearable.*;
import co.edu.uniquindio.kinet.services.WearableService;
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

@Tag(name = "06. Wearables", description = "Conexión con proveedores e importación de actividades")
@RestController
@RequestMapping("/api/wearables/conexiones")
@RequiredArgsConstructor
public class WearableController {

    private final WearableService wearableService;

    @Operation(summary = "Mis conexiones")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<ConexionDTO>>> misConexiones(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, wearableService.misConexiones(auth.getName())));
    }

    @Operation(summary = "Conectar un proveedor (el token se guarda cifrado)")
    @PostMapping
    public ResponseEntity<ResponseDTO<ConexionDTO>> conectar(Authentication auth, @Valid @RequestBody ConectarWearableDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, wearableService.conectar(auth.getName(), dto)));
    }

    @Operation(summary = "Revocar una conexión (se borra el token)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<ConexionDTO>> revocar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, wearableService.revocar(auth.getName(), id)));
    }

    @Operation(summary = "Importar actividades del proveedor (las repetidas se ignoran)")
    @PostMapping("/{id}/sincronizaciones")
    public ResponseEntity<ResponseDTO<ResultadoSincronizacionDTO>> sincronizar(Authentication auth, @PathVariable Long id,
                                                                              @Valid @RequestBody SincronizacionDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, wearableService.sincronizar(auth.getName(), id, dto)));
    }
}
