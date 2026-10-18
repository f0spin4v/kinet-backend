package co.edu.uniquindio.kinet.mappers;

import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.TipoActividadDTO;
import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.TipoActividad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntrenamientoMapper {

    @Mapping(target = "codigoActividad", source = "tipoActividad.codigo")
    @Mapping(target = "actividad", source = "tipoActividad.nombre")
    EntrenamientoDTO toEntrenamientoDTO(Entrenamiento entrenamiento);

    TipoActividadDTO toTipoActividadDTO(TipoActividad tipoActividad);
}
