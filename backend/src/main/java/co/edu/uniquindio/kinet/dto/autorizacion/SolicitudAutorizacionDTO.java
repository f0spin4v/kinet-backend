package co.edu.uniquindio.kinet.dto.autorizacion;

import co.edu.uniquindio.kinet.model.AutorizacionMenor;

import java.time.LocalDateTime;

/**
 * Lo que ve el representante al abrir el enlace: a quién autoriza y hasta cuándo puede responder.
 * Solo el nombre de pila del menor y su gamertag; nada de correos ni contraseñas.
 */
public record SolicitudAutorizacionDTO(
        String nombreMenor,
        String gamertag,
        int edad,
        String nombreRepresentante,
        String parentesco,
        LocalDateTime fechaExpiracion
) {
    public static SolicitudAutorizacionDTO de(AutorizacionMenor a, int edad) {
        return new SolicitudAutorizacionDTO(a.getUsuario().getNombres(),
                a.getUsuario().getPerfil() == null ? null : a.getUsuario().getPerfil().getGamertag(), edad,
                a.getNombreRepresentante(), a.getParentesco().name(), a.getFechaExpiracion());
    }
}
