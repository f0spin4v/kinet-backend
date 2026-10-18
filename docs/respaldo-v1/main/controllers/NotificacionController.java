package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.services.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "7. Notificaciones", description = "Avisos de entrenamientos, niveles y retos")
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Mis últimas 30 notificaciones")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<NotificacionDTO>>> listar(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, notificacionService.listar(autenticacion.getName())));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<ResponseDTO<NotificacionDTO>> marcarLeida(Authentication autenticacion, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, notificacionService.marcarLeida(autenticacion.getName(), id)));
    }
}
