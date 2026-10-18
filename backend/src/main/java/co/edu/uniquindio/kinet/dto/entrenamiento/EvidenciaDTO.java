package co.edu.uniquindio.kinet.dto.entrenamiento;

import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;

import java.time.LocalDateTime;

/**
 * Datos de una evidencia y de su entrenamiento, sin la imagen (se descarga aparte). tipo es IMAGEN o ENLACE; en un
 * enlace vienen url y plataforma, y en una imagen el archivo.
 */
public record EvidenciaDTO(
        Long id,
        Long idEntrenamiento,
        Long idUsuario,
        String gamertag,
        String actividad,
        Integer duracionMin,
        EstadoValidacion estadoEntrenamiento,
        String tipo,
        String nombreArchivo,
        String tipoContenido,
        Integer tamanoBytes,
        String url,
        String plataforma,
        EstadoEvidencia estadoRevision,
        String notaRevision,
        LocalDateTime fechaSubida,
        LocalDateTime fechaRevision
) {
}
