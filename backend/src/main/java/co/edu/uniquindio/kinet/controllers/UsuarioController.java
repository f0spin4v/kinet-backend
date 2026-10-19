package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.usuario.CambiarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.usuario.EditarPerfilDTO;
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

@Tag(name = "02. Perfil", description = "Cuenta y perfil del usuario autenticado")
@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Ver mis datos y mi perfil")
    @GetMapping
    public ResponseEntity<ResponseDTO<UsuarioDTO>> obtener(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.obtener(auth.getName())));
    }

    @Operation(summary = "Editar mi perfil")
    @PutMapping
    public ResponseEntity<ResponseDTO<UsuarioDTO>> editar(Authentication auth, @Valid @RequestBody EditarPerfilDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.editarPerfil(auth.getName(), dto)));
    }

    @Operation(summary = "Cambiar mi contraseña")
    @PutMapping("/contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cambiarContrasena(Authentication auth,
                                                                     @Valid @RequestBody CambiarContrasenaDTO dto) {
        usuarioService.cambiarContrasena(auth.getName(), dto);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Tu contraseña fue actualizada.")));
    }

    @Operation(summary = "Desactivar mi cuenta (queda INACTIVA; soporte puede reactivarla)")
    @DeleteMapping
    public ResponseEntity<ResponseDTO<MensajeDTO>> desactivar(Authentication auth) {
        usuarioService.desactivar(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Tu cuenta quedó inactiva.")));
    }
}
