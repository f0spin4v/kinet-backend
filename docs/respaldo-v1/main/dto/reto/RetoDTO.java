package co.edu.uniquindio.kinet.dto.reto;

/** Reto del catálogo. "participacion" trae el avance del jugador si ya se unió, o null. */
public record RetoDTO(
        Long id,
        String titulo,
        String descripcion,
        String tipoMeta,
        Integer metaValor,
        String codigoActividad,
        String actividad,
        String nivelRequerido,
        Integer duracionDias,
        Integer xpRecompensa,
        Boolean activo,
        ParticipacionDTO participacion
) {
}
