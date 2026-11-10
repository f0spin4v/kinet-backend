package co.edu.uniquindio.kinet.dto.autorizacion;

import co.edu.uniquindio.kinet.model.AutorizacionMenor;

import java.time.LocalDateTime;

/** Autorización de un menor, como la ve el administrador. */
public record AutorizacionMenorDTO(
        Long idUsuario,
        String nombreRepresentante,
        String correoRepresentante,
        String parentesco,
        String estado,
        String canal,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaExpiracion,
        LocalDateTime fechaRespuesta,
        String soporte
) {
    public static AutorizacionMenorDTO de(AutorizacionMenor a) {
        return new AutorizacionMenorDTO(a.getUsuario().getId(), a.getNombreRepresentante(), a.getCorreoRepresentante(),
                a.getParentesco().name(), a.getEstado().name(), a.getCanal() == null ? null : a.getCanal().name(),
                a.getFechaSolicitud(), a.getFechaExpiracion(), a.getFechaRespuesta(), a.getSoporte());
    }
}
