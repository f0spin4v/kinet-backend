package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.progreso.AjusteXpDTO;
import co.edu.uniquindio.kinet.dto.progreso.MovimientoXpDTO;
import co.edu.uniquindio.kinet.dto.usuario.CambiarEstadoUsuarioDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.services.ProgresoService;
import co.edu.uniquindio.kinet.services.UsuarioService;
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

@Tag(name = "26. Soporte", description = "Búsqueda de cuentas, estado de cuentas y ajustes de XP")
@RestController
@RequestMapping("/api/soporte")
@RequiredArgsConstructor
public class SoporteController {

    private final UsuarioService usuarioService;
    private final ProgresoService progresoService;

    @Operation(summary = "Buscar cuentas por correo (parcial) y estado")
    @GetMapping("/usuarios")
    public ResponseEntity<ResponseDTO<PaginaDTO<UsuarioDTO>>> buscar(@RequestParam(required = false) String correo,
                                                                     @RequestParam(required = false) EstadoUsuario estado,
                                                                     @RequestParam(defaultValue = "0") int pagina,
                                                                     @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false,
                usuarioService.buscar(correo, estado, PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Activar, inactivar o bloquear una cuenta")
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<ResponseDTO<UsuarioDTO>> cambiarEstado(Authentication auth, @PathVariable Long id,
                                                                 @Valid @RequestBody CambiarEstadoUsuarioDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.cambiarEstado(auth.getName(), id, dto.estado())));
    }

    @Operation(summary = "Ajuste manual de XP (con motivo)")
    @PostMapping("/ajustes-xp")
    public ResponseEntity<ResponseDTO<MovimientoXpDTO>> ajustar(@Valid @RequestBody AjusteXpDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, progresoService.ajustar(dto)));
    }
}
