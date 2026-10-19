package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.auth.*;
import co.edu.uniquindio.kinet.exceptions.AccesoDenegadoException;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.CredencialesInvalidasException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.TokenRecuperacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.CanalNotificacion;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.PerfilJugadorRepository;
import co.edu.uniquindio.kinet.repositories.RolRepository;
import co.edu.uniquindio.kinet.repositories.TokenRecuperacionRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Proceso 1: registro, inicio de sesión y recuperación de contraseña. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PerfilJugadorRepository perfilRepository;
    private final RolRepository rolRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    @Value("${kinet.recuperacion.expiracion-minutos}")
    private long minutosRecuperacion;

    /** Crea la cuenta como JUGADOR con su perfil de juego en cero. */
    @Transactional
    public SesionDTO registrar(RegistroDTO registroDTO) {
        String correo = UsuarioService.normalizar(registroDTO.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña.");
        }
        if (perfilRepository.existsByGamertag(registroDTO.gamertag())) {
            throw new ConflictoException("Ese gamertag ya está en uso. Elige otro.");
        }
        Rol jugador = rolRepository.findByNombre(Rol.JUGADOR)
                .orElseThrow(() -> new IllegalStateException("Falta el rol JUGADOR en la base de datos."));

        Usuario usuario = Usuario.builder()
                .rol(jugador)
                .nombres(registroDTO.nombres().trim())
                .apellidos(registroDTO.apellidos().trim())
                .correo(correo)
                .passwordHash(passwordEncoder.encode(registroDTO.contrasena()))
                .aceptaPoliticaDatos(true)
                .fechaAceptaPolitica(LocalDate.now())
                .build();
        usuario.setPerfil(PerfilJugador.builder()
                .usuario(usuario)
                .gamertag(registroDTO.gamertag())
                .fechaNacimiento(registroDTO.fechaNacimiento())
                .build());
        usuarioRepository.save(usuario);
        return sesion(usuario);
    }

    @Transactional(readOnly = true)
    public SesionDTO iniciarSesion(LoginDTO loginDTO) {
        // mismo mensaje si falla el correo o la contraseña, para no revelar qué cuentas existen
        Usuario usuario = usuarioRepository.findByCorreo(UsuarioService.normalizar(loginDTO.correo()))
                .filter(u -> passwordEncoder.matches(loginDTO.contrasena(), u.getPasswordHash()))
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));
        if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new AccesoDenegadoException("Tu cuenta está bloqueada. Contacta a soporte.");
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new AccesoDenegadoException("Tu cuenta está inactiva. Contacta a soporte para reactivarla.");
        }
        return sesion(usuario);
    }

    /** Siempre responde igual, exista o no la cuenta, para no revelar qué correos están registrados. */
    @Transactional
    public void solicitarRecuperacion(RecuperarContrasenaDTO recuperarDTO) {
        usuarioRepository.findByCorreo(UsuarioService.normalizar(recuperarDTO.correo())).ifPresent(usuario -> {
            LocalDateTime ahora = LocalDateTime.now();
            // un enlace nuevo invalida los anteriores
            tokenRepository.invalidarPendientes(usuario.getId(), ahora);

            String token = generarToken();
            tokenRepository.save(TokenRecuperacion.builder()
                    .usuario(usuario)
                    .tokenHash(hash(token))
                    .fechaCreacion(ahora)
                    .fechaExpiracion(ahora.plusMinutes(minutosRecuperacion))
                    .build());
            emailService.enviarRecuperacion(usuario.getCorreo(), usuario.getNombres(), token);
            notificacionService.notificar(usuario, TipoNotificacion.RECUPERACION, "Recuperación de contraseña",
                    "Te enviamos un enlace para restablecer tu contraseña. Si no lo pediste, ignóralo.");
        });
    }

    @Transactional
    public void restablecerContrasena(RestablecerContrasenaDTO restablecerDTO) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(hash(restablecerDTO.token()))
                .filter(TokenRecuperacion::estaVigente)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El enlace de recuperación no es válido o ya venció. Solicita uno nuevo."));
        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(restablecerDTO.nuevaContrasena()));
        token.setFechaUso(LocalDateTime.now());
        notificacionService.notificar(usuario, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                "Contraseña restablecida", "Tu contraseña fue restablecida. Si no fuiste tú, contacta a soporte.");
    }

    private SesionDTO sesion(Usuario usuario) {
        String token = jwtUtils.generarToken(usuario.getCorreo(), usuario.getRol().getNombre());
        return new SesionDTO(token, usuarioService.toDTO(usuario));
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
