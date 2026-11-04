package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.usuario.CambiarRolDTO;
import co.edu.uniquindio.kinet.dto.usuario.CrearCuentaDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
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

@Tag(name = "24. Admin: usuarios", description = "Creación de cuentas y roles")
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Crear una cuenta con su rol (el titular acepta la política de datos en su primer ingreso)")
    @PostMapping
    public ResponseEntity<ResponseDTO<UsuarioDTO>> crear(Authentication auth, @Valid @RequestBody CrearCuentaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, usuarioService.crearCuenta(auth.getName(), dto)));
    }

    @Operation(summary = "Cambiar el rol de una cuenta")
    @PatchMapping("/{id}/rol")
    public ResponseEntity<ResponseDTO<UsuarioDTO>> cambiarRol(Authentication auth, @PathVariable Long id,
                                                              @Valid @RequestBody CambiarRolDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.cambiarRol(auth.getName(), id, dto.rol())));
    }
}
