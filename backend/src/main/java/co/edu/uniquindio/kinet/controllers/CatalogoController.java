package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.catalogo.*;
import co.edu.uniquindio.kinet.services.CatalogoService;
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

@Tag(name = "04. Catálogos", description = "Actividades, niveles y ligas (lectura)")
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @Operation(summary = "Actividades activas con su XP por minuto vigente")
    @GetMapping("/actividades")
    public ResponseEntity<ResponseDTO<List<ActividadDTO>>> actividades() {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.actividades()));
    }

    @Operation(summary = "Niveles de habilidad")
    @GetMapping("/niveles-habilidad")
    public ResponseEntity<ResponseDTO<List<NivelHabilidadDTO>>> nivelesHabilidad() {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.nivelesHabilidad()));
    }

    @Operation(summary = "XP necesario para cada nivel")
    @GetMapping("/niveles-progresion")
    public ResponseEntity<ResponseDTO<List<NivelProgresionDTO>>> nivelesProgresion() {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.nivelesProgresion()));
    }

    @Operation(summary = "Ligas con sus rangos")
    @GetMapping("/ligas")
    public ResponseEntity<ResponseDTO<List<LigaDTO>>> ligas() {
        return ResponseEntity.ok(new ResponseDTO<>(false, catalogoService.ligas()));
    }
}
