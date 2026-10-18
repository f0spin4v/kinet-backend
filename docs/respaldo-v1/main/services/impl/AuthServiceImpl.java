package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.auth.*;
import co.edu.uniquindio.kinet.exceptions.AccesoDenegadoException;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.CredencialesInvalidasException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.Perfil;
import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.TokenRecuperacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.PerfilRepository;
import co.edu.uniquindio.kinet.repositories.RolRepository;
import co.edu.uniquindio.kinet.repositories.TokenRecuperacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.security.JwtUtils;
import co.edu.uniquindio.kinet.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final RolRepository rolRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UsuarioService usuarioService;
    private final ProgresoService progresoService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    @Value("${kinet.recuperacion.expiracion-minutos}")
    private long minutosRecuperacion;

    @Override
    @Transactional
    public SesionDTO registrar(RegistroDTO registroDTO) {
        String correo = normalizar(registroDTO.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            // RNF-26: un correo, una sola cuenta
            throw new ConflictoException("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña.");
        }
        if (perfilRepository.existsByNickname(registroDTO.nickname())) {
            throw new ConflictoException("Ese nickname ya está en uso. Elige otro.");
        }
        Rol jugador = rolRepository.findByNombre(Rol.JUGADOR)
                .orElseThrow(() -> new IllegalStateException("Falta el rol JUGADOR en la base de datos."));

        Usuario usuario = Usuario.builder()
                .nombre(registroDTO.nombre().trim())
                .correo(correo)
                .contrasenaHash(passwordEncoder.encode(registroDTO.contrasena()))
                .rol(jugador)
                .aceptaDatos(true)
                .fechaAceptaDatos(LocalDateTime.now())
                .build();
        usuario.setPerfil(Perfil.builder().usuario(usuario).nickname(registroDTO.nickname()).build());
        usuarioRepository.save(usuario);
        progresoService.obtenerOCrear(usuario);

        return sesion(usuario);
    }

    @Override
    @Transactional
    public SesionDTO iniciarSesion(LoginDTO loginDTO) {
        // mismo mensaje si falla el correo o la contraseña, para no revelar qué cuentas existen
        Usuario usuario = usuarioRepository.findByCorreo(normalizar(loginDTO.correo()))
                .filter(u -> passwordEncoder.matches(loginDTO.contrasena(), u.getContrasenaHash()))
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));
        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("Tu cuenta está bloqueada. Contacta a soporte.");
        }
        usuario.setUltimoAcceso(LocalDateTime.now());
        return sesion(usuario);
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(RecuperarContrasenaDTO recuperarDTO) {
        usuarioRepository.findByCorreo(normalizar(recuperarDTO.correo())).ifPresent(usuario -> {
            // un enlace nuevo invalida los anteriores
            tokenRepository.findByUsuarioIdAndUsado(usuario.getId(), false).forEach(t -> t.setUsado(true));

            String token = generarToken();
            tokenRepository.save(TokenRecuperacion.builder()
                    .usuario(usuario)
                    .tokenHash(hash(token))
                    .fechaExpiracion(LocalDateTime.now().plusMinutes(minutosRecuperacion))
                    .build());
            emailService.enviarRecuperacion(usuario.getCorreo(), usuario.getNombre(), token);
        });
    }

    @Override
    @Transactional
    public void restablecerContrasena(RestablecerContrasenaDTO restablecerDTO) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(hash(restablecerDTO.token()))
                .filter(TokenRecuperacion::estaVigente)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El enlace de recuperación no es válido o ya venció. Solicita uno nuevo."));
        Usuario usuario = token.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(restablecerDTO.nuevaContrasena()));
        token.setUsado(true);
        notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD,
                "Tu contraseña fue restablecida. Si no fuiste tú, contacta a soporte.");
    }

    private SesionDTO sesion(Usuario usuario) {
        String token = jwtUtils.generarToken(usuario.getCorreo(), usuario.getRol().getNombre());
        return new SesionDTO(token, usuarioService.toUsuarioDTO(usuario));
    }

    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 del token: si alguien lee la base de datos, no puede usar los enlaces. */
    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
