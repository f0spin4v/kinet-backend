package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;

import java.util.List;

/** Avisos al jugador: se guardan en la app y se envían al proveedor externo de notificaciones. */
public interface NotificacionService {

    /**
     * Encola un aviso y regresa de inmediato. Se guarda en otro hilo cuando la transacción actual se confirma;
     * si se deshace, el aviso no se crea.
     */
    void notificar(Usuario usuario, TipoNotificacion tipo, String mensaje);

    List<NotificacionDTO> listar(String correo);

    NotificacionDTO marcarLeida(String correo, Long idNotificacion);
}
