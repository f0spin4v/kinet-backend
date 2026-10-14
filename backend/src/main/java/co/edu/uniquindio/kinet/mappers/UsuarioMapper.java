package co.edu.uniquindio.kinet.mappers;

import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UsuarioMapper {

    /** nivelInicial viene aparte porque se guarda en la evaluación, no en el usuario. */
    @Mapping(target = "id", source = "usuario.id")
    @Mapping(target = "nombre", source = "usuario.nombre")
    @Mapping(target = "correo", source = "usuario.correo")
    @Mapping(target = "rol", source = "usuario.rol.nombre")
    @Mapping(target = "nickname", source = "usuario.perfil.nickname")
    @Mapping(target = "avatarUrl", source = "usuario.perfil.avatarUrl")
    @Mapping(target = "fechaNacimiento", source = "usuario.perfil.fechaNacimiento")
    @Mapping(target = "ciudad", source = "usuario.perfil.ciudad")
    @Mapping(target = "deporteFavorito", source = "usuario.perfil.deporteFavorito")
    @Mapping(target = "nivelInicial", source = "nivelInicial")
    UsuarioDTO toUsuarioDTO(Usuario usuario, String nivelInicial);
}
