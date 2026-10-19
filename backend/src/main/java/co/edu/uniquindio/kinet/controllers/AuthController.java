package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.auth.*;
import co.edu.uniquindio.kinet.services.AuthService;
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

@Tag(name = "01. Autenticación", description = "Registro, inicio de sesión y recuperación de contraseña")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Crear una cuenta de jugador")
    @PostMapping("/registro")
    public ResponseEntity<ResponseDTO<SesionDTO>> registrar(@Valid @RequestBody RegistroDTO registroDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, authService.registrar(registroDTO)));
    }

    @Operation(summary = "Iniciar sesión")
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<SesionDTO>> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(new ResponseDTO<>(false, authService.iniciarSesion(loginDTO)));
    }

    @Operation(summary = "Pedir un enlace para restablecer la contraseña")
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> recuperar(@Valid @RequestBody RecuperarContrasenaDTO dto) {
        authService.solicitarRecuperacion(dto);
        return ResponseEntity.ok(new ResponseDTO<>(false,
                new MensajeDTO("Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.")));
    }

    @Operation(summary = "Restablecer la contraseña con el enlace recibido")
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> restablecer(@Valid @RequestBody RestablecerContrasenaDTO dto) {
        authService.restablecerContrasena(dto);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Tu contraseña fue restablecida.")));
    }
}
