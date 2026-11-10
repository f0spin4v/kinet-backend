package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.RespuestaAutorizacionDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.SolicitudAutorizacionDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.TokenAutorizacionDTO;
import co.edu.uniquindio.kinet.services.AutorizacionMenorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Público: el representante legal abre el enlace del correo sin tener cuenta en Kinet (RN-33). */
@Tag(name = "01b. Autorización del representante", description = "Titulares de 14 a 17 años (Ley 1581, Decreto 1377 art. 12)")
@RestController
@RequestMapping("/api/autorizaciones")
@RequiredArgsConstructor
public class AutorizacionController {

    private final AutorizacionMenorService autorizacionService;

    @Operation(summary = "Ver a quién se autoriza con el enlace del correo")
    @PostMapping("/consulta")
    public ResponseEntity<ResponseDTO<SolicitudAutorizacionDTO>> consultar(@Valid @RequestBody TokenAutorizacionDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, autorizacionService.consultar(dto.token())));
    }

    @Operation(summary = "Autorizar o no la cuenta del menor")
    @PostMapping("/respuesta")
    public ResponseEntity<ResponseDTO<MensajeDTO>> responder(@Valid @RequestBody RespuestaAutorizacionDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false,
                new MensajeDTO(autorizacionService.responder(dto.token(), dto.autoriza()))));
    }
}
