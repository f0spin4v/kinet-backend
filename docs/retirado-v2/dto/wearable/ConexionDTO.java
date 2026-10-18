package co.edu.uniquindio.kinet.dto.wearable;

import co.edu.uniquindio.kinet.model.ConexionWearable;
import java.time.LocalDateTime;

/** Conexión con un proveedor. Nunca incluye el token. */
public record ConexionDTO(Long id, Long idProveedor, String proveedor, String idCuentaExterna, String estado,
                          LocalDateTime fechaVinculacion) {
    public static ConexionDTO de(ConexionWearable c) {
        return new ConexionDTO(c.getId(), c.getProveedor().getId(), c.getProveedor().getNombre(),
                c.getIdCuentaExterna(), c.getEstado().name(), c.getFechaVinculacion());
    }
}
