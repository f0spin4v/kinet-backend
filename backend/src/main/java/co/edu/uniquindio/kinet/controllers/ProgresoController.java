package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.services.ProgresoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. Progreso", description = "Proceso 2: datos del dashboard (UI-003)")
@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
public class ProgresoController {

    private final ProgresoService progresoService;

    @Operation(summary = "XP, nivel, racha, semana y retos del jugador")
    @GetMapping
    public ResponseEntity<ResponseDTO<ProgresoDTO>> resumen(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, progresoService.resumen(autenticacion.getName())));
    }
}
