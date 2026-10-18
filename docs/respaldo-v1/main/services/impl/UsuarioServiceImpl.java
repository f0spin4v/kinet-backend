package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.usuario.CambiarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.usuario.EditarPerfilDTO;
import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.mappers.UsuarioMapper;
import co.edu.uniquindio.kinet.model.Perfil;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.repositories.EvaluacionNivelRepository;
import co.edu.uniquindio.kinet.repositories.PerfilRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final EvaluacionNivelRepository evaluacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerEntidad(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO toUsuarioDTO(Usuario usuario) {
        String nivel = evaluacionRepository.findByUsuarioId(usuario.getId())
                .map(e -> e.getNivelInicial().name())
                .orElse(null);
        return usuarioMapper.toUsuarioDTO(usuario, nivel);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO obtener(String correo) {
        return toUsuarioDTO(obtenerEntidad(correo));
    }

    @Override
    @Transactional
    public UsuarioDTO editarPerfil(String correo, EditarPerfilDTO editarDTO) {
        Usuario usuario = obtenerEntidad(correo);
        if (perfilRepository.existsByNicknameAndUsuarioIdNot(editarDTO.nickname(), usuario.getId())) {
            throw new ConflictoException("Ese nickname ya está en uso. Elige otro.");
        }
        Perfil perfil = usuario.getPerfil();
        if (perfil == null) {
            perfil = Perfil.builder().usuario(usuario).build();
            usuario.setPerfil(perfil);
        }
        usuario.setNombre(editarDTO.nombre().trim());
        perfil.setNickname(editarDTO.nickname());
        perfil.setAvatarUrl(textoOpcional(editarDTO.avatarUrl()));
        perfil.setFechaNacimiento(editarDTO.fechaNacimiento());
        perfil.setCiudad(textoOpcional(editarDTO.ciudad()));
        perfil.setDeporteFavorito(textoOpcional(editarDTO.deporteFavorito()));
        usuarioRepository.save(usuario);
        return toUsuarioDTO(usuario);
    }

    @Override
    @Transactional
    public void cambiarContrasena(String correo, CambiarContrasenaDTO cambiarDTO) {
        Usuario usuario = obtenerEntidad(correo);
        if (!passwordEncoder.matches(cambiarDTO.contrasenaActual(), usuario.getContrasenaHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        usuario.setContrasenaHash(passwordEncoder.encode(cambiarDTO.nuevaContrasena()));
        usuarioRepository.save(usuario);
    }

    /** Los campos opcionales vacíos se guardan como null. */
    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
