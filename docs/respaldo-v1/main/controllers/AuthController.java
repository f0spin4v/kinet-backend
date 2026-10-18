package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.auth.*;
import co.edu.uniquindio.kinet.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Autenticación", description = "Proceso 1: registro, inicio de sesión y recuperación de contraseña")
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

    @Operation(summary = "Iniciar sesión y obtener el token")
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<SesionDTO>> iniciarSesion(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(new ResponseDTO<>(false, authService.iniciarSesion(loginDTO)));
    }

    @Operation(summary = "Pedir el enlace para restablecer la contraseña")
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> recuperar(@Valid @RequestBody RecuperarContrasenaDTO recuperarDTO) {
        authService.solicitarRecuperacion(recuperarDTO);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO(
                "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.")));
    }

    @Operation(summary = "Crear una contraseña nueva con el código del enlace")
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<ResponseDTO<MensajeDTO>> restablecer(@Valid @RequestBody RestablecerContrasenaDTO restablecerDTO) {
        authService.restablecerContrasena(restablecerDTO);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO(
                "Tu contraseña fue actualizada. Ya puedes iniciar sesión.")));
    }
}
