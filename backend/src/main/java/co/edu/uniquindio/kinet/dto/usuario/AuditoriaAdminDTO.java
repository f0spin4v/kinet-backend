package co.edu.uniquindio.kinet.dto.usuario;

import co.edu.uniquindio.kinet.model.AuditoriaAdmin;

import java.time.LocalDateTime;

/** Una acción del administrador en la bitácora. */
public record AuditoriaAdminDTO(
        Long id,
        String correoActor,
        String accion,
        Long idUsuarioAfectado,
        String correoAfectado,
        String valorAnterior,
        String valorNuevo,
        LocalDateTime fecha
) {
    public static AuditoriaAdminDTO de(AuditoriaAdmin a) {
        return new AuditoriaAdminDTO(a.getId(), a.getCorreoActor(), a.getAccion(), a.getIdUsuarioAfectado(),
                a.getCorreoAfectado(), a.getValorAnterior(), a.getValorNuevo(), a.getFecha());
    }
}
