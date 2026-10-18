package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.clan.ClanDTO;
import co.edu.uniquindio.kinet.dto.clan.ClanDetalleDTO;
import co.edu.uniquindio.kinet.dto.clan.GuardarClanDTO;
import co.edu.uniquindio.kinet.dto.clan.TransferirLiderDTO;
import co.edu.uniquindio.kinet.services.ClanService;
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

@Tag(name = "08. Clanes", description = "Crear, unirse, salir y administrar un clan")
@RestController
@RequestMapping("/api/clanes")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;

    @Operation(summary = "Clanes activos")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<ClanDTO>>> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, clanService.listar(PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Crear un clan (quedo como líder)")
    @PostMapping
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> crear(Authentication auth, @Valid @RequestBody GuardarClanDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, clanService.crear(auth.getName(), dto)));
    }

    @Operation(summary = "Mi clan")
    @GetMapping("/mio")
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> miClan(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, clanService.miClan(auth.getName())));
    }

    @Operation(summary = "Salir de mi clan")
    @PostMapping("/mio/salida")
    public ResponseEntity<ResponseDTO<MensajeDTO>> salir(Authentication auth) {
        clanService.salir(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("Saliste del clan.")));
    }

    @Operation(summary = "Detalle de un clan con sus miembros")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, clanService.detalle(id)));
    }

    @Operation(summary = "Editar el clan (líder)")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> editar(Authentication auth, @PathVariable Long id,
                                                              @Valid @RequestBody GuardarClanDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, clanService.editar(auth.getName(), id, dto)));
    }

    @Operation(summary = "Unirme a un clan")
    @PostMapping("/{id}/miembros")
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> unirse(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(false, clanService.unirse(auth.getName(), id)));
    }

    @Operation(summary = "Expulsar a un miembro (líder)")
    @DeleteMapping("/{id}/miembros/{idUsuario}")
    public ResponseEntity<ResponseDTO<MensajeDTO>> expulsar(Authentication auth, @PathVariable Long id,
                                                            @PathVariable Long idUsuario) {
        clanService.expulsar(auth.getName(), id, idUsuario);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El miembro salió del clan.")));
    }

    @Operation(summary = "Pasar el liderazgo a otro miembro (líder)")
    @PutMapping("/{id}/lider")
    public ResponseEntity<ResponseDTO<ClanDetalleDTO>> transferir(Authentication auth, @PathVariable Long id,
                                                                  @Valid @RequestBody TransferirLiderDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, clanService.transferirLiderazgo(auth.getName(), id, dto.idUsuario())));
    }

    @Operation(summary = "Cerrar el clan (líder)")
    @PostMapping("/{id}/cierre")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cerrar(Authentication auth, @PathVariable Long id) {
        clanService.cerrar(auth.getName(), id, false);
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El clan quedó cerrado.")));
    }
}
