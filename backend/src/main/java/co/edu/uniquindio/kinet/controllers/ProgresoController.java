package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.progreso.MovimientoXpDTO;
import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.services.ProgresoService;
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

@Tag(name = "12. Progreso", description = "Dashboard (UI-003) y libro de XP")
@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
public class ProgresoController {

    private final ProgresoService progresoService;

    @Operation(summary = "XP, nivel, racha, semana, retos, logros y rango")
    @GetMapping
    public ResponseEntity<ResponseDTO<ProgresoDTO>> resumen(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, progresoService.resumen(auth.getName())));
    }

    @Operation(summary = "Mis movimientos de XP, del más reciente al más antiguo")
    @GetMapping("/movimientos")
    public ResponseEntity<ResponseDTO<PaginaDTO<MovimientoXpDTO>>> movimientos(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, progresoService.movimientos(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }
}
