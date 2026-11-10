package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.autorizacion.AutorizacionMenorDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.RegistrarAutorizacionDTO;
import co.edu.uniquindio.kinet.dto.autorizacion.SolicitudAutorizacionDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.AuditoriaAdmin;
import co.edu.uniquindio.kinet.model.AutorizacionMenor;
import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.*;
import co.edu.uniquindio.kinet.repositories.AutorizacionMenorRepository;
import co.edu.uniquindio.kinet.repositories.PerfilJugadorRepository;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import co.edu.uniquindio.kinet.security.TokensSeguros;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RN-33: titulares de 14 a 17 años. Su cuenta queda PENDIENTE hasta que el representante legal autoriza el
 * tratamiento de sus datos (Ley 1581 de 2012, art. 7; Decreto 1377 de 2013, art. 12), respondiendo el enlace del
 * correo o con un documento firmado que registra el administrador. Si no autoriza, la cuenta queda INACTIVO.
 */
@Service
@RequiredArgsConstructor
public class AutorizacionMenorService {

    /** Días que tiene el representante para responder el correo. */
    public static final int DIAS_ENLACE = 7;
    private static final String ENLACE_INVALIDO =
            "El enlace de autorización no es válido o ya venció. El menor puede pedir que te lo enviemos de nuevo.";

    private final AutorizacionMenorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilJugadorRepository perfilRepository;
    private final UsuarioService usuarioService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final BitacoraService bitacoraService;

    /** Al registrarse un menor: guarda la solicitud y envía el enlace a su representante. */
    @Transactional
    public void solicitar(Usuario menor, String nombreRepresentante, String correoRepresentante, Parentesco parentesco) {
        String token = TokensSeguros.generar();
        repository.save(AutorizacionMenor.builder()
                .usuario(menor)
                .nombreRepresentante(nombreRepresentante.trim())
                .correoRepresentante(UsuarioService.normalizar(correoRepresentante))
                .parentesco(parentesco)
                .tokenHash(TokensSeguros.hash(token))
                .fechaExpiracion(LocalDateTime.now().plusDays(DIAS_ENLACE))
                .build());
        emailService.enviarAutorizacionMenor(UsuarioService.normalizar(correoRepresentante), nombreRepresentante.trim(),
                menor.getNombres(), token);
    }

    /** Lo que ve el representante al abrir el enlace. */
    @Transactional(readOnly = true)
    public SolicitudAutorizacionDTO consultar(String token) {
        AutorizacionMenor autorizacion = vigente(token);
        PerfilJugador perfil = autorizacion.getUsuario().getPerfil();
        int edad = perfil == null || perfil.getFechaNacimiento() == null ? 0 : UsuarioService.edad(perfil.getFechaNacimiento());
        return SolicitudAutorizacionDTO.de(autorizacion, edad);
    }

    /** El representante autoriza (la cuenta queda ACTIVO) o no (queda INACTIVO). El enlace sirve una sola vez. */
    @Transactional
    public String responder(String token, boolean autoriza) {
        AutorizacionMenor autorizacion = vigente(token);
        Usuario menor = autorizacion.getUsuario();
        autorizacion.setTokenHash(null);
        autorizacion.setCanal(CanalAutorizacion.CORREO);
        autorizacion.setFechaRespuesta(LocalDateTime.now());
        if (autoriza) {
            autorizacion.setEstado(EstadoAutorizacion.AUTORIZADA);
            activar(menor);
            notificacionService.notificar(menor, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                    "Tu cuenta está lista", "Tu representante legal autorizó tu cuenta de Kinet. Ya puedes iniciar sesión.");
        } else {
            autorizacion.setEstado(EstadoAutorizacion.RECHAZADA);
            menor.setEstado(EstadoUsuario.INACTIVO);
            notificacionService.notificar(menor, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                    "Tu cuenta no fue autorizada", "Tu representante legal no autorizó tu cuenta de Kinet, así que no se "
                            + "puede usar. Si fue un error, pídele que escriba a soporte.");
        }
        repository.save(autorizacion);
        usuarioRepository.save(menor);
        return autoriza ? "Gracias. La cuenta quedó autorizada y ya se puede usar."
                : "Listo. La cuenta no se autorizó y no se podrá usar.";
    }

    /** Envía un enlace nuevo si la cuenta sigue PENDIENTE. No revela si el correo existe. */
    @Transactional
    public void reenviar(String correo) {
        usuarioRepository.findByCorreo(UsuarioService.normalizar(correo))
                .filter(u -> u.getEstado() == EstadoUsuario.PENDIENTE)
                .flatMap(u -> repository.findByUsuarioId(u.getId()))
                .filter(a -> a.getEstado() == EstadoAutorizacion.PENDIENTE)
                .ifPresent(a -> {
                    String token = TokensSeguros.generar();
                    a.setTokenHash(TokensSeguros.hash(token));
                    a.setFechaExpiracion(LocalDateTime.now().plusDays(DIAS_ENLACE));
                    repository.save(a);
                    emailService.enviarAutorizacionMenor(a.getCorreoRepresentante(), a.getNombreRepresentante(),
                            a.getUsuario().getNombres(), token);
                });
    }

    @Transactional(readOnly = true)
    public AutorizacionMenorDTO obtener(Long idUsuario) {
        return repository.findByUsuarioId(idUsuario).map(AutorizacionMenorDTO::de)
                .orElseThrow(() -> new NoEncontradoException("La cuenta no tiene una autorización de representante."));
    }

    /**
     * El administrador registra una autorización firmada (por ejemplo, en papel) que guarda el equipo.
     * Solo para titulares de 14 a 17 años. Queda en la bitácora.
     */
    @Transactional
    public AutorizacionMenorDTO registrar(String correoActor, Long idUsuario, RegistrarAutorizacionDTO dto) {
        Usuario actor = usuarioService.obtenerEntidad(correoActor);
        Usuario menor = usuarioService.obtenerPorId(idUsuario);
        PerfilJugador perfil = usuarioService.perfilDe(menor);
        LocalDate nacimiento = perfil.getFechaNacimiento() != null ? perfil.getFechaNacimiento() : dto.fechaNacimiento();
        if (nacimiento == null) {
            throw new ReglaNegocioException("El perfil no tiene fecha de nacimiento: escríbela para verificar la edad.");
        }
        UsuarioService.validarEdad(nacimiento);
        if (!UsuarioService.esMenor(nacimiento)) {
            throw new ReglaNegocioException("La autorización del representante solo aplica a titulares de 14 a 17 años.");
        }
        if (perfil.getFechaNacimiento() == null) {
            perfil.setFechaNacimiento(nacimiento);
            perfilRepository.save(perfil);
        }
        LocalDateTime ahora = LocalDateTime.now();
        AutorizacionMenor autorizacion = repository.findByUsuarioId(idUsuario)
                .orElseGet(() -> AutorizacionMenor.builder().usuario(menor).fechaExpiracion(ahora).build());
        if (autorizacion.getEstado() == EstadoAutorizacion.AUTORIZADA) {
            throw new ConflictoException("La cuenta ya tiene una autorización vigente.");
        }
        Parentesco parentesco = Parentesco.valueOf(dto.parentesco());
        autorizacion.setNombreRepresentante(dto.nombreRepresentante().trim());
        autorizacion.setCorreoRepresentante(UsuarioService.normalizar(dto.correoRepresentante()));
        autorizacion.setParentesco(parentesco);
        autorizacion.setEstado(EstadoAutorizacion.AUTORIZADA);
        autorizacion.setCanal(CanalAutorizacion.ADMINISTRADOR);
        autorizacion.setSoporte(dto.soporte().trim());
        autorizacion.setIdAdmin(actor.getId());
        autorizacion.setTokenHash(null);
        autorizacion.setFechaRespuesta(ahora);
        String anterior = menor.getEstado().name();
        activar(menor);
        repository.save(autorizacion);
        usuarioRepository.save(menor);
        bitacoraService.registrar(actor, AuditoriaAdmin.AUTORIZACION_MENOR, menor, anterior,
                "AUTORIZADA (" + parentesco.name() + "): " + dto.soporte().trim());
        notificacionService.notificar(menor, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                "Tu cuenta está lista", "Registramos la autorización de tu representante legal. Ya puedes iniciar sesión.");
        return AutorizacionMenorDTO.de(autorizacion);
    }

    /** El representante retiró la autorización: la cuenta queda INACTIVO y el motivo, en la bitácora. */
    @Transactional
    public AutorizacionMenorDTO revocar(String correoActor, Long idUsuario, String motivo) {
        Usuario actor = usuarioService.obtenerEntidad(correoActor);
        AutorizacionMenor autorizacion = repository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new NoEncontradoException("La cuenta no tiene una autorización de representante."));
        if (autorizacion.getEstado() != EstadoAutorizacion.AUTORIZADA) {
            throw new ReglaNegocioException("Solo se puede revocar una autorización vigente.");
        }
        Usuario menor = autorizacion.getUsuario();
        autorizacion.setEstado(EstadoAutorizacion.REVOCADA);
        autorizacion.setIdAdmin(actor.getId());
        autorizacion.setFechaRespuesta(LocalDateTime.now());
        menor.setEstado(EstadoUsuario.INACTIVO);
        menor.setAceptaPoliticaDatos(false);
        repository.save(autorizacion);
        usuarioRepository.save(menor);
        bitacoraService.registrar(actor, AuditoriaAdmin.AUTORIZACION_MENOR, menor, "AUTORIZADA", "REVOCADA: " + motivo.trim());
        notificacionService.notificar(menor, TipoNotificacion.SEGURIDAD, CanalNotificacion.EMAIL,
                "Autorización retirada", "Tu representante legal retiró su autorización, así que tu cuenta de Kinet "
                        + "quedó inactiva.");
        return AutorizacionMenorDTO.de(autorizacion);
    }

    private AutorizacionMenor vigente(String token) {
        return repository.findByTokenHash(TokensSeguros.hash(token))
                .filter(AutorizacionMenor::enlaceVigente)
                .orElseThrow(() -> new ReglaNegocioException(ENLACE_INVALIDO));
    }

    /** La autorización del representante también es el consentimiento sobre la política de datos. */
    private static void activar(Usuario menor) {
        menor.setEstado(EstadoUsuario.ACTIVO);
        menor.setAceptaPoliticaDatos(true);
        menor.setFechaAceptaPolitica(LocalDate.now());
    }
}
