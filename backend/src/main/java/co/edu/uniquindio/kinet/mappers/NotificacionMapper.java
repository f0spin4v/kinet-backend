package co.edu.uniquindio.kinet.mappers;

import co.edu.uniquindio.kinet.dto.notificacion.NotificacionDTO;
import co.edu.uniquindio.kinet.model.Notificacion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificacionMapper {
    NotificacionDTO toNotificacionDTO(Notificacion notificacion);
}
