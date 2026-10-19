package co.edu.uniquindio.kinet.dto.usuario;

import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos de la cuenta y del perfil. Nunca incluye la contraseña. nivelHabilidad es null sin evaluación. */
public record UsuarioDTO(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String estado,
        String gamertag,
        String avatarUrl,
        LocalDate fechaNacimiento,
        String nivelHabilidad,
        LocalDateTime fechaRegistro
) {
    public static UsuarioDTO de(Usuario usuario, String nivelHabilidad) {
        PerfilJugador perfil = usuario.getPerfil();
        return new UsuarioDTO(usuario.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getCorreo(),
                usuario.getRol().getNombre(), usuario.getEstado().name(),
                perfil == null ? null : perfil.getGamertag(),
                perfil == null ? null : perfil.getAvatarUrl(),
                perfil == null ? null : perfil.getFechaNacimiento(),
                nivelHabilidad, usuario.getFechaRegistro());
    }
}
