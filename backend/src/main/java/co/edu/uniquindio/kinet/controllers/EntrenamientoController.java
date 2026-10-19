package co.edu.uniquindio.kinet.controllers;

import co.edu.uniquindio.kinet.dto.MensajeDTO;
import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.*;
import co.edu.uniquindio.kinet.services.EntrenamientoService;
import co.edu.uniquindio.kinet.services.EvidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "05. Entrenamientos", description = "Cronómetro en el servidor, anti-trampa, XP y evidencia opcional")
@RestController
@RequestMapping("/api/entrenamientos")
@RequiredArgsConstructor
public class EntrenamientoController {

    private final EntrenamientoService entrenamientoService;
    private final EvidenciaService evidenciaService;

    @Operation(summary = "Iniciar entrenamiento: el servidor guarda la hora de inicio")
    @PostMapping("/en-curso")
    public ResponseEntity<ResponseDTO<EntrenamientoDTO>> iniciar(Authentication auth,
                                                                 @Valid @RequestBody IniciarEntrenamientoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, entrenamientoService.iniciar(auth.getName(), dto)));
    }

    @Operation(summary = "Ver el entrenamiento en curso (con los minutos transcurridos). Sin cronómetro abierto responde 200 con respuesta null")
    @GetMapping("/en-curso")
    public ResponseEntity<ResponseDTO<EntrenamientoDTO>> enCurso(Authentication auth) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.enCurso(auth.getName())));
    }

    @Operation(summary = "Finalizar entrenamiento: el servidor guarda la hora de fin, calcula la duración y otorga el XP")
    @PostMapping("/en-curso/fin")
    public ResponseEntity<ResponseDTO<ResultadoEntrenamientoDTO>> finalizar(Authentication auth,
                                                                            @Valid @RequestBody FinalizarEntrenamientoDTO dto) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.finalizar(auth.getName(), dto)));
    }

    @Operation(summary = "Cancelar el entrenamiento en curso (no se registra nada)")
    @DeleteMapping("/en-curso")
    public ResponseEntity<ResponseDTO<MensajeDTO>> cancelar(Authentication auth) {
        entrenamientoService.cancelar(auth.getName());
        return ResponseEntity.ok(new ResponseDTO<>(false, new MensajeDTO("El entrenamiento se canceló.")));
    }

    @Operation(summary = "Mi historial, del más reciente al más antiguo")
    @GetMapping
    public ResponseEntity<ResponseDTO<PaginaDTO<EntrenamientoDTO>>> historial(Authentication auth,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.historial(auth.getName(), PaginaDTO.solicitar(pagina, tamanio))));
    }

    @Operation(summary = "Ver un entrenamiento mío")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<EntrenamientoDTO>> obtener(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(new ResponseDTO<>(false, entrenamientoService.obtener(auth.getName(), id)));
    }

    @Operation(summary = "Subir una imagen como evidencia (opcional, JPEG/PNG/WEBP de hasta 5 MB, da bonus de XP)")
    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<EvidenciaDTO>> subirEvidencia(Authentication auth, @PathVariable Long id,
                                                                    @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, evidenciaService.subir(auth.getName(), id, archivo)));
    }

    @Operation(summary = "Subir el enlace de la actividad en una app deportiva reconocida (Strava, Garmin Connect...). "
            + "Obligatorio en sesiones de más de 180 min (4 horas para subirlo) y para competir; da bonus de XP")
    @PostMapping("/{id}/evidencia/enlace")
    public ResponseEntity<ResponseDTO<EvidenciaDTO>> subirEnlace(Authentication auth, @PathVariable Long id,
                                                                 @Valid @RequestBody EvidenciaEnlaceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDTO<>(false, evidenciaService.subirEnlace(auth.getName(), id, dto.url())));
    }

    @Operation(summary = "Ver la imagen de mi evidencia (404 si la evidencia es un enlace)")
    @GetMapping("/{id}/evidencia")
    public ResponseEntity<byte[]> verEvidencia(Authentication auth, @PathVariable Long id) {
        return imagen(evidenciaService.archivoPropio(auth.getName(), id));
    }

    static ResponseEntity<byte[]> imagen(EvidenciaService.Archivo archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivo.nombre().replace("\"", "") + "\"")
                .body(archivo.contenido());
    }
}
