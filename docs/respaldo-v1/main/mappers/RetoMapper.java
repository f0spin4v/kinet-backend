package co.edu.uniquindio.kinet.mappers;

import co.edu.uniquindio.kinet.dto.reto.ParticipacionDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.model.ParticipacionReto;
import co.edu.uniquindio.kinet.model.Reto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RetoMapper {

    @Mapping(target = "id", source = "reto.id")
    @Mapping(target = "titulo", source = "reto.titulo")
    @Mapping(target = "descripcion", source = "reto.descripcion")
    @Mapping(target = "tipoMeta", source = "reto.tipoMeta")
    @Mapping(target = "metaValor", source = "reto.metaValor")
    @Mapping(target = "codigoActividad", source = "reto.tipoActividad.codigo")
    @Mapping(target = "actividad", source = "reto.tipoActividad.nombre")
    @Mapping(target = "nivelRequerido", source = "reto.nivelRequerido")
    @Mapping(target = "duracionDias", source = "reto.duracionDias")
    @Mapping(target = "xpRecompensa", source = "reto.xpRecompensa")
    @Mapping(target = "activo", source = "reto.activo")
    @Mapping(target = "participacion", source = "participacion")
    RetoDTO toRetoDTO(Reto reto, ParticipacionDTO participacion);

    @Mapping(target = "idReto", source = "reto.id")
    @Mapping(target = "tituloReto", source = "reto.titulo")
    @Mapping(target = "meta", source = "reto.metaValor")
    @Mapping(target = "porcentaje", expression = "java(porcentaje(participacion))")
    ParticipacionDTO toParticipacionDTO(ParticipacionReto participacion);

    default Integer porcentaje(ParticipacionReto participacion) {
        int meta = participacion.getReto().getMetaValor();
        return Math.min(100, participacion.getProgreso() * 100 / meta);
    }
}
