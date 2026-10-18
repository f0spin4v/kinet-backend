package co.edu.uniquindio.kinet.controllers;

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

@Tag(name = "11. Logros", description = "Catálogo de logros con los que ya obtuve")
@RestController
@RequestMapping("/api/logros")
@RequiredArgsConstructor
public class LogroController {

    private final LogroService logroService;

    @Operation(summary = "Logros y cuáles ya desbloqueé")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<LogroDTO>>> catalogo(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, logroService.catalogo(auth.getName())));
    }
}
