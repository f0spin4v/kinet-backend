package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.usuario.CambiarRolDTO;
import co.edu.uniquindio.kinet.dto.usuario.CrearCuentaDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.AutorizacionMenorDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.RegistrarAutorizacionDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.RevocarAutorizacionDTO;
import co.edu.uniquindio.kinet.services.AutorizacionMenorService;
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
    private final AutorizacionMenorService autorizacionService;

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

    @Operation(summary = "Ver la autorización del representante legal de un menor")
    @GetMapping("/{id}/autorizacion-menor")
    public ResponseEntity<ResponseDTO<AutorizacionMenorDTO>> verAutorizacion(@PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, autorizacionService.obtener(id)));
    }

    @Operation(summary = "Registrar la autorización firmada del representante legal de un menor de 14 a 17 años")
    @PostMapping("/{id}/autorizacion-menor")
    public ResponseEntity<ResponseDTO<AutorizacionMenorDTO>> registrarAutorizacion(Authentication auth, @PathVariable Long id,
                                                                                  @Valid @RequestBody RegistrarAutorizacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, autorizacionService.registrar(auth.getName(), id, dto)));
    }

    @Operation(summary = "Revocar la autorización del representante (la cuenta queda inactiva)")
    @PostMapping("/{id}/autorizacion-menor/revocacion")
    public ResponseEntity<ResponseDTO<AutorizacionMenorDTO>> revocarAutorizacion(Authentication auth, @PathVariable Long id,
                                                                                @Valid @RequestBody RevocarAutorizacionDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, autorizacionService.revocar(auth.getName(), id, dto.motivo())));
    }
}
