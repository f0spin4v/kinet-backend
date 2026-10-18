package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.temporada.ParticipacionTemporadaDTO;
import co.edu.uniquindio.kinet.dto.temporada.PosicionRankingDTO;
import co.edu.uniquindio.kinet.dto.temporada.TemporadaDTO;
import co.edu.uniquindio.kinet.dto.temporada.HistorialRangoDTO;
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

@Tag(name = "09. Temporadas", description = "Temporada activa, ranking y mi rango")
@RestController
@RequestMapping("/api/temporadas/activa")
@RequiredArgsConstructor
public class TemporadaController {

    private final TemporadaService temporadaService;
    private final CierreTemporadaService cierreTemporadaService;

    @Operation(summary = "Temporada activa")
    @GetMapping
    public ResponseEntity<ResponseDTO<TemporadaDTO>> activa() {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.activa()));
    }

    @Operation(summary = "Ranking de la temporada activa")
    @GetMapping("/ranking")
    public ResponseEntity<ResponseDTO<List<PosicionRankingDTO>>> ranking(@RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.ranking(Math.min(Math.max(limite, 1), 200))));
    }

    @Operation(summary = "Mis puntos, rango y posición")
    @GetMapping("/mia")
    public ResponseEntity<ResponseDTO<ParticipacionTemporadaDTO>> mia(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.miParticipacion(auth.getName())));
    }

    @Operation(summary = "Mis cambios de rango en la temporada activa")
    @GetMapping("/mia/historial-rango")
    public ResponseEntity<ResponseDTO<PaginaDTO<HistorialRangoDTO>>> historialRango(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, temporadaService.miHistorialRango(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Abandonar la temporada activa (no se puede deshacer)")
    @PostMapping("/abandono")
    public ResponseEntity<ResponseDTO<ParticipacionTemporadaDTO>> abandonar(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, cierreTemporadaService.abandonar(auth.getName())));
    }
}
