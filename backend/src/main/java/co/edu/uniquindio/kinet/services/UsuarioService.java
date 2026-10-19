package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.usuario.CambiarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.usuario.EditarPerfilDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.EvaluacionUsuarioRepository;
import co.edu.uniquindio.kinet.repositories.IdTexto;
import co.edu.uniquindio.kinet.repositories.PerfilJugadorRepository;
import co.edu.uniquindio.kinet.repositories.RolRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cuenta y perfil del usuario autenticado, y la gestión de cuentas que hace el administrador. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilJugadorRepository perfilRepository;
    private final RolRepository rolRepository;
    private final EvaluacionUsuarioRepository evaluacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public Usuario obtenerEntidad(String correo) {
        return usuarioRepository.findByCorreo(normalizar(correo))
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."));
    }

    /** Varios usuarios (con rol y perfil) en el orden pedido. Falla si alguno no existe. */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerVarios(Collection<Long> idsUsuario) {
        Map<Long, Usuario> porId = new HashMap<>();
        usuarioRepository.conRolYPerfil(idsUsuario).forEach(u -> porId.put(u.getId(), u));
        return idsUsuario.stream().map(id -> {
            Usuario usuario = porId.get(id);
            if (usuario == null) {
                throw new NoEncontradoException("El usuario no existe.");
            }
            return usuario;
        }).toList();
    }

    /** Perfil de juego del usuario. Toda cuenta lo tiene desde el registro. */
    @Transactional(readOnly = true)
    public PerfilJugador perfilDe(Usuario usuario) {
        return perfilRepository.findById(usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("El usuario no tiene perfil de jugador."));
    }

    @Transactional(readOnly = true)
    public UsuarioDTO toDTO(Usuario usuario) {
        return UsuarioDTO.de(usuario, nivelesVigentes(List.of(usuario.getId())).get(usuario.getId()));
    }

    /** Gamertag de cada usuario, en una sola consulta. */
    @Transactional(readOnly = true)
    public Map<Long, String> gamertags(Collection<Long> idsUsuario) {
        return porId(idsUsuario.isEmpty() ? List.of() : perfilRepository.gamertags(new HashSet<>(idsUsuario)));
    }

    @Transactional(readOnly = true)
    public String gamertag(Long idUsuario) {
        return gamertags(List.of(idUsuario)).get(idUsuario);
    }

    private Map<Long, String> nivelesVigentes(Collection<Long> idsUsuario) {
        return porId(idsUsuario.isEmpty() ? List.of() : evaluacionRepository.nivelesVigentes(idsUsuario));
    }

    private static Map<Long, String> porId(List<IdTexto> filas) {
        Map<Long, String> mapa = new HashMap<>();
        filas.forEach(f -> mapa.put(f.getId(), f.getTexto()));
        return mapa;
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtener(String correo) {
        return toDTO(obtenerEntidad(correo));
    }

    @Transactional
    public UsuarioDTO editarPerfil(String correo, EditarPerfilDTO editarDTO) {
        Usuario usuario = obtenerEntidad(correo);
        if (perfilRepository.existsByGamertagAndIdNot(editarDTO.gamertag(), usuario.getId())) {
            throw new ConflictoException("Ese gamertag ya está en uso. Elige otro.");
        }
        PerfilJugador perfil = perfilDe(usuario);
        usuario.setNombres(editarDTO.nombres().trim());
        usuario.setApellidos(editarDTO.apellidos().trim());
        perfil.setGamertag(editarDTO.gamertag());
        perfil.setAvatarUrl(textoOpcional(editarDTO.avatarUrl()));
        perfil.setFechaNacimiento(editarDTO.fechaNacimiento());
        usuarioRepository.save(usuario);
        perfilRepository.save(perfil);
        return toDTO(usuario);
    }

    @Transactional
    public void cambiarContrasena(String correo, CambiarContrasenaDTO cambiarDTO) {
        Usuario usuario = obtenerEntidad(correo);
        if (!passwordEncoder.matches(cambiarDTO.contrasenaActual(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(cambiarDTO.nuevaContrasena()));
        usuarioRepository.save(usuario);
        notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                "Contraseña actualizada", "Tu contraseña de Kinet se cambió. Si no fuiste tú, contacta a soporte.");
    }

    /** El jugador desactiva su propia cuenta (INACTIVO). Soporte puede reactivarla. */
    @Transactional
    public void desactivar(String correo) {
        Usuario usuario = obtenerEntidad(correo);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    // ---------- administración ----------

    @Transactional(readOnly = true)
    public PaginaDTO<UsuarioDTO> buscar(String correo, EstadoUsuario estado, Pageable pagina) {
        String filtro = correo == null || correo.isBlank() ? "" : normalizar(correo);
        Page<Usuario> usuarios = estado == null ? usuarioRepository.buscar(filtro, pagina)
                : usuarioRepository.buscar(filtro, estado, pagina);
        return PaginaDTO.de(usuarios, lista -> {
            Map<Long, String> niveles = nivelesVigentes(lista.stream().map(Usuario::getId).toList());
            return lista.stream().map(u -> UsuarioDTO.de(u, niveles.get(u.getId()))).toList();
        });
    }

    /** El administrador activa, inactiva o bloquea cuentas. Nadie cambia su propia cuenta. */
    @Transactional
    public UsuarioDTO cambiarEstado(String correoActor, Long idUsuario, EstadoUsuario estado) {
        Usuario actor = obtenerEntidad(correoActor);
        Usuario usuario = obtenerPorId(idUsuario);
        if (actor.getId().equals(usuario.getId())) {
            throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta.");
        }
        usuario.setEstado(estado);
        usuarioRepository.save(usuario);
        notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                "Estado de tu cuenta", "Tu cuenta de Kinet ahora está " + estado.name() + ".");
        return toDTO(usuario);
    }

    @Transactional
    public UsuarioDTO cambiarRol(String correoActor, Long idUsuario, String nombreRol) {
        Usuario actor = obtenerEntidad(correoActor);
        Usuario usuario = obtenerPorId(idUsuario);
        if (actor.getId().equals(usuario.getId())) {
            throw new ReglaNegocioException("No puedes cambiar tu propio rol.");
        }
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new NoEncontradoException("El rol no existe."));
        usuario.setRol(rol);
        return toDTO(usuarioRepository.save(usuario));
    }

    static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
