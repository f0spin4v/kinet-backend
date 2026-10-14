package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.usuario.CambiarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.usuario.EditarPerfilDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. Perfil", description = "Proceso 1: datos del usuario autenticado (UI-002)")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Ver mis datos y mi perfil")
    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<UsuarioDTO>> obtener(Authentication autenticacion) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.obtener(autenticacion.getName())));
    }

    @Operation(summary = "Editar mi perfil")
    @PutMapping("/me")
    public ResponseEntity<ResponseDTO<UsuarioDTO>> editar(Authentication autenticacion,
                                                          @Valid @RequestBody EditarPerfilDTO editarDTO) {
        return ResponseEntity.ok(new ResponseDTO<>(false, usuarioService.editarPerfil(autenticacion.getName(), editarDTO)));
    }

    @Operation(summary = "Cambiar mi contraseña")
    @PutMapping("/me/contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cambiarContrasena(Authentication autenticacion,
                                                                     @Valid @RequestBody CambiarContrasenaDTO cambiarDTO) {
        usuarioService.cambiarContrasena(autenticacion.getName(), cambiarDTO);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Tu contraseña fue actualizada.")));
    }
}
