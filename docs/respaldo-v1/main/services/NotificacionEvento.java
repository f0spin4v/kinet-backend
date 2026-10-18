package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;

import java.time.LocalDateTime;

/**
 * Aviso pendiente de guardar y enviar. Lleva el id del usuario y no la entidad, porque se procesa en otro hilo
 * fuera de la sesión de JPA de la petición. La fecha se fija al publicarlo para conservar el orden de los avisos.
 */
public record NotificacionEvento(Long idUsuario, String correo, TipoNotificacion tipo, String mensaje,
                                 LocalDateTime fecha) {
}
