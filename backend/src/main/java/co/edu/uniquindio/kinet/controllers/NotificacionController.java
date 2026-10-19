package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.services.NotificacionService;
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

@Tag(name = "13. Notificaciones", description = "Bandeja de avisos")
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Mis notificaciones, de la más reciente a la más antigua")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<NotificacionDTO>>> listar(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, notificacionService.listar(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<ResponseDTO<NotificacionDTO>> marcarLeida(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, notificacionService.marcarLeida(auth.getName(), id)));
    }

    @Operation(summary = "Borrar una notificación de mi bandeja")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<MensajeDTO>> eliminar(Authentication auth, @PathVariable Long id) {
        notificacionService.eliminar(auth.getName(), id);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Notificación borrada.")));
    }

    @Operation(summary = "Vaciar mi bandeja")
    @DeleteMapping
    public ResponseEntity<ResponseDTO<MensajeDTO>> eliminarTodas(Authentication auth) {
        int borradas = notificacionService.eliminarTodas(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO(borradas + " notificaciones borradas.")));
    }

    @Operation(summary = "Marcar toda la bandeja como leída")
    @PatchMapping("/leidas")
    public ResponseEntity<ResponseDTO<MensajeDTO>> marcarTodas(Authentication auth) {
        int cambiadas = notificacionService.marcarTodasLeidas(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO(cambiadas + " notificaciones marcadas como leídas.")));
    }
}
