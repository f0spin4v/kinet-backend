package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.usuario.AuditoriaAdminDTO;
import co.edu.uniquindio.kinet.services.BitacoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "27. Admin: bitácora", description = "Quién cambió estados, roles y cuentas, y cuándo")
@RestController
@RequestMapping("/api/admin/bitacora")
@RequiredArgsConstructor
public class AdminBitacoraController {

    private final BitacoraService bitacoraService;

    @Operation(summary = "Acciones administrativas, lo más reciente primero")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<AuditoriaAdminDTO>>> listar(
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, bitacoraService.listar(PaginaDTO.solicitar(pagina, tamanio))));
    }
}
