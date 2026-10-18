package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.usuario.CambiarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.usuario.EditarPerfilDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.model.Usuario;

/** Proceso 1: consulta y gestión del perfil del usuario autenticado. */
public interface UsuarioService {

    /** Busca la entidad del usuario por correo; lanza NoEncontradoException si no existe. */
    Usuario obtenerEntidad(String correo);

    UsuarioDTO toUsuarioDTO(Usuario usuario);

    UsuarioDTO obtener(String correo);

    UsuarioDTO editarPerfil(String correo, EditarPerfilDTO editarDTO);

    void cambiarContrasena(String correo, CambiarContrasenaDTO cambiarDTO);
}
