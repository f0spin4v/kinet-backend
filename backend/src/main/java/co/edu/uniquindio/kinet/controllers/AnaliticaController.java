package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.analitica.ResumenAnaliticaDTO;
import co.edu.uniquindio.kinet.services.AnaliticaService;
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

@Tag(name = "27. Analítica", description = "Indicadores agregados")
@RestController
@RequestMapping("/api/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @Operation(summary = "Resumen de usuarios, entrenamientos, XP, retos, ligas y notificaciones")
    @GetMapping("/resumen")
    public ResponseEntity<ResponseDTO<ResumenAnaliticaDTO>> resumen() {
        return ResponseEntity.ok(new ResponseDTO<>(false, analiticaService.resumen()));
    }
}
