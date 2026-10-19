package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.IdValor;
import co.edu.uniquindio.kinet.repositories.MovimientoXpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Proceso 2 con cronómetro en el servidor: "Iniciar" guarda la hora de inicio del servidor y "Finalizar" la de
 * fin, así que la duración no se puede escribir a mano. Al finalizar: anti-trampa -> XP según REGLA_PUNTUACION
 * -> racha -> aportes a retos -> logros -> notificación. Lo sospechoso queda PENDIENTE para un entrenador.
 */
@Service
@RequiredArgsConstructor
public class EntrenamientoService {

    private final EntrenamientoRepository entrenamientoRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final UsuarioService usuarioService;
    private final CatalogoService catalogoService;
    private final AntiTrampaService antiTrampaService;
    private final ProgresoService progresoService;
    private final RetoService retoService;
    private final LogroService logroService;
    private final EvidenciaService evidenciaService;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    // ---------- cronómetro ----------

    /** "Iniciar entrenamiento": crea la sesión EN_CURSO con la hora del servidor. Solo una a la vez. */
    @Transactional
    public EntrenamientoDTO iniciar(String correo, IniciarEntrenamientoDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        TipoActividad actividad = catalogoService.actividadActiva(dto.idTipoActividad());
        if (entrenamientoRepository.findFirstByUsuarioIdAndEstadoValidacion(usuario.getId(), EstadoValidacion.EN_CURSO)
                .isPresent()) {
            throw new ConflictoException("Ya tienes un entrenamiento en curso: finalízalo o cancélalo primero.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Entrenamiento entrenamiento = entrenamientoRepository.save(Entrenamiento.builder()
                .usuario(usuario)
                .tipoActividad(actividad)
                .fechaHoraInicio(ahora)
                .fechaRegistro(ahora)
                .build());
        return dto(entrenamiento, null, null);
    }

    @Transactional(readOnly = true)
    public EntrenamientoDTO enCurso(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return entrenamientoRepository.findFirstByUsuarioIdAndEstadoValidacion(usuario.getId(), EstadoValidacion.EN_CURSO)
                .map(e -> dto(e, null, null))
                .orElse(null);
    }

    /** "Finalizar entrenamiento": el servidor pone la hora de fin y calcula la duración. */
    @Transactional
    public ResultadoEntrenamientoDTO finalizar(String correo, FinalizarEntrenamientoDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Entrenamiento entrenamiento = enCursoDe(usuario);
        LocalDateTime fin = LocalDateTime.now(reloj);
        int minutos = (int) Duration.between(entrenamiento.getFechaHoraInicio(), fin).toMinutes();
        ReglaPuntuacion regla = catalogoService.reglaVigente(entrenamiento.getTipoActividad(), dto.intensidad(),
                entrenamiento.getFechaHoraInicio().toLocalDate());
        AntiTrampaService.Veredicto veredicto = antiTrampaService.validar(usuario, entrenamiento.getFechaHoraInicio(),
                minutos, regla.getDuracionMinimaMin());

        entrenamiento.setFechaHoraFin(fin);
        entrenamiento.setDuracionMin(minutos);
        entrenamiento.setIntensidad(dto.intensidad());
        entrenamiento.setDistanciaKm(dto.distanciaKm());
        entrenamiento.setCaloriasKcal(dto.caloriasKcal());
        entrenamiento.setSeries(dto.series());
        entrenamiento.setRepeticiones(dto.repeticiones());
        entrenamiento.setPesoKg(dto.pesoKg());
        entrenamiento.setNotas(textoOpcional(dto.notas()));
        entrenamiento.setEstadoValidacion(veredicto.estado());
        entrenamiento.setMotivoRechazo(veredicto.motivo());
        entrenamientoRepository.save(entrenamiento);

        switch (veredicto.estado()) {
            case RECHAZADO -> notificacionService.notificar(usuario, TipoNotificacion.VALIDACION,
                    "Entrenamiento rechazado", "Tu entrenamiento no fue validado: " + veredicto.motivo());
            case PENDIENTE -> notificacionService.notificar(usuario, TipoNotificacion.VALIDACION,
                    "Entrenamiento en revisión", "Tu sesión de " + minutos + " minutos quedó pendiente de revisión "
                            + "por un entrenador. Sube el enlace de la actividad en tu app deportiva (Strava, Garmin "
                            + "Connect...) en las próximas " + ReglasJuego.HORAS_EVIDENCIA_SESION_LARGA
                            + " horas o la sesión se rechazará.");
            case VALIDADO -> {
                return aplicarValidado(usuario, entrenamiento, regla);
            }
            default -> throw new IllegalStateException("Veredicto inesperado: " + veredicto.estado());
        }
        return sinXp(usuario, entrenamiento);
    }

    /** Descarta el cronómetro en marcha sin registrar nada. */
    @Transactional
    public void cancelar(String correo) {
        entrenamientoRepository.delete(enCursoDe(usuarioService.obtenerEntidad(correo)));
    }

    /**
     * Cronómetros que siguen corriendo después de 5 horas: se cierran como RECHAZADO (el jugador olvidó
     * finalizarlos). Lo llama la tarea programada.
     */
    @Transactional
    public int cerrarAbandonados() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Entrenamiento> abandonados = entrenamientoRepository.findByEstadoValidacionAndFechaHoraInicioBefore(
                EstadoValidacion.EN_CURSO, ahora.minusMinutes(ReglasJuego.MINUTOS_CIERRE_AUTOMATICO));
        for (Entrenamiento e : abandonados) {
            e.setFechaHoraFin(ahora);
            e.setDuracionMin((int) Duration.between(e.getFechaHoraInicio(), ahora).toMinutes());
            e.setEstadoValidacion(EstadoValidacion.RECHAZADO);
            e.setMotivoRechazo("El cronómetro se cerró solo: pasaron más de " + ReglasJuego.MINUTOS_CIERRE_AUTOMATICO / 60
                    + " horas sin finalizarlo.");
            entrenamientoRepository.save(e);
            notificacionService.notificar(e.getUsuario(), TipoNotificacion.VALIDACION, "Entrenamiento cerrado",
                    e.getMotivoRechazo());
        }
        return abandonados.size();
    }

    /**
     * Sesiones pendientes (más de 180 minutos) que terminaron hace más de 4 horas sin un enlace de evidencia: se
     * rechazan solas. Lo llama la tarea programada.
     */
    @Transactional
    public int rechazarSinEvidencia() {
        List<Entrenamiento> vencidas = entrenamientoRepository.sinEnlaceTerminadasAntesDe(EstadoValidacion.PENDIENTE,
                LocalDateTime.now(reloj).minusHours(ReglasJuego.HORAS_EVIDENCIA_SESION_LARGA), EstadoEvidencia.RECHAZADA);
        for (Entrenamiento e : vencidas) {
            e.setEstadoValidacion(EstadoValidacion.RECHAZADO);
            e.setMotivoRechazo("Una sesión de más de " + ReglasJuego.UMBRAL_REVISION + " minutos necesita el enlace de "
                    + "la actividad en una app deportiva, y no llegó en " + ReglasJuego.HORAS_EVIDENCIA_SESION_LARGA
                    + " horas.");
            entrenamientoRepository.save(e);
            notificacionService.notificar(e.getUsuario(), TipoNotificacion.VALIDACION, "Entrenamiento rechazado",
                    e.getMotivoRechazo());
        }
        return vencidas.size();
    }

    // ---------- consultas ----------

    @Transactional(readOnly = true)
    public PaginaDTO<EntrenamientoDTO> historial(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return PaginaDTO.de(
                entrenamientoRepository.findByUsuarioIdOrderByFechaHoraInicioDescIdDesc(usuario.getId(), pagina),
                this::dtos);
    }

    @Transactional(readOnly = true)
    public EntrenamientoDTO obtener(String correo, Long idEntrenamiento) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Entrenamiento entrenamiento = entrenamientoRepository.findByIdAndUsuarioId(idEntrenamiento, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("El entrenamiento no existe."));
        return dtos(List.of(entrenamiento)).get(0);
    }

    // ---------- moderación ----------

    @Transactional(readOnly = true)
    public PaginaDTO<EntrenamientoPendienteDTO> pendientes(Pageable pagina) {
        return PaginaDTO.de(entrenamientoRepository.findByEstadoValidacionOrderByFechaRegistroAscIdAsc(
                EstadoValidacion.PENDIENTE, pagina), pendientes -> {
            List<EntrenamientoDTO> dtos = dtos(pendientes);
            Map<Long, String> gamertags =
                    usuarioService.gamertags(pendientes.stream().map(e -> e.getUsuario().getId()).toList());
            List<EntrenamientoPendienteDTO> resultado = new ArrayList<>();
            for (int i = 0; i < pendientes.size(); i++) {
                Long idJugador = pendientes.get(i).getUsuario().getId();
                resultado.add(new EntrenamientoPendienteDTO(idJugador, gamertags.get(idJugador), dtos.get(i)));
            }
            return resultado;
        });
    }

    /**
     * El entrenador aprueba (se otorga el XP con la regla vigente el día de la sesión, y el bonus de la evidencia si la
     * tiene) o rechaza con motivo.
     */
    @Transactional
    public ResultadoEntrenamientoDTO revisar(Long idEntrenamiento, RevisionEntrenamientoDTO revision) {
        Entrenamiento entrenamiento = entrenamientoRepository.findById(idEntrenamiento)
                .orElseThrow(() -> new NoEncontradoException("El entrenamiento no existe."));
        if (entrenamiento.getEstadoValidacion() != EstadoValidacion.PENDIENTE) {
            throw new ReglaNegocioException("Este entrenamiento no está pendiente de revisión.");
        }
        Usuario usuario = entrenamiento.getUsuario();
        if (Boolean.TRUE.equals(revision.aprobar())) {
            if (!evidenciaService.tieneEnlaceVigente(idEntrenamiento)) {
                throw new ReglaNegocioException("No se puede aprobar: la sesión necesita el enlace de la actividad en "
                        + "una app deportiva.");
            }
            entrenamiento.setEstadoValidacion(EstadoValidacion.VALIDADO);
            entrenamiento.setMotivoRechazo(null);
            entrenamientoRepository.save(entrenamiento);
            ReglaPuntuacion regla = catalogoService.reglaVigente(entrenamiento.getTipoActividad(),
                    entrenamiento.getIntensidad(), entrenamiento.getFechaHoraInicio().toLocalDate());
            return aplicarValidado(usuario, entrenamiento, regla);
        }
        if (revision.motivo() == null || revision.motivo().isBlank()) {
            throw new ReglaNegocioException("Escribe el motivo del rechazo.");
        }
        entrenamiento.setEstadoValidacion(EstadoValidacion.RECHAZADO);
        entrenamiento.setMotivoRechazo(revision.motivo().trim());
        entrenamientoRepository.save(entrenamiento);
        notificacionService.notificar(usuario, TipoNotificacion.VALIDACION, "Entrenamiento rechazado",
                "Un entrenador no validó tu entrenamiento: " + revision.motivo().trim());
        return sinXp(usuario, entrenamiento);
    }

    // ---------- flujo ----------

    private ResultadoEntrenamientoDTO aplicarValidado(Usuario usuario, Entrenamiento entrenamiento, ReglaPuntuacion regla) {
        int xpAntes = usuarioService.perfilDe(usuario).getXpTotal();
        int nivelAntes = catalogoService.nivelPara(xpAntes);

        int xp = ReglasJuego.calcularXp(entrenamiento.getDuracionMin(), regla.getXpPorMinuto());
        progresoService.porEntrenamiento(usuario, entrenamiento, xp);
        int racha = progresoService.actualizarRacha(usuario, entrenamiento.getFechaHoraInicio().toLocalDate());
        List<ParticipacionReto> completados = retoService.registrarAportes(entrenamiento);
        evidenciaService.otorgarBonusSiCorresponde(entrenamiento);
        List<String> logros = logroService.evaluar(usuario);
        notificacionService.notificar(usuario, TipoNotificacion.VALIDACION, "Entrenamiento validado",
                entrenamiento.getTipoActividad().getNombre() + ", " + entrenamiento.getDuracionMin() + " min. +" + xp + " XP.");

        int xpTotal = usuarioService.perfilDe(usuario).getXpTotal();
        int nivel = catalogoService.nivelPara(xpTotal);
        return new ResultadoEntrenamientoDTO(
                dtos(List.of(entrenamiento)).get(0),
                xpTotal - xpAntes,
                xpTotal,
                nivel,
                nivel > nivelAntes,
                racha,
                completados.stream().map(p -> p.getReto().getNombre()).toList(),
                logros);
    }

    private ResultadoEntrenamientoDTO sinXp(Usuario usuario, Entrenamiento entrenamiento) {
        PerfilJugador perfil = usuarioService.perfilDe(usuario);
        return new ResultadoEntrenamientoDTO(dtos(List.of(entrenamiento)).get(0), 0, perfil.getXpTotal(),
                catalogoService.nivelPara(perfil.getXpTotal()), false, progresoService.rachaVigente(perfil),
                List.of(), List.of());
    }

    private Entrenamiento enCursoDe(Usuario usuario) {
        return entrenamientoRepository.findFirstByUsuarioIdAndEstadoValidacion(usuario.getId(), EstadoValidacion.EN_CURSO)
                .orElseThrow(() -> new NoEncontradoException("No tienes un entrenamiento en curso."));
    }

    private EntrenamientoDTO dto(Entrenamiento e, Integer xp, String evidencia) {
        return EntrenamientoDTO.de(e, xp, evidencia, LocalDateTime.now(reloj));
    }

    /** Arma los DTO con el XP de cada entrenamiento y el estado de su evidencia (dos consultas en total). */
    private List<EntrenamientoDTO> dtos(List<Entrenamiento> entrenamientos) {
        List<Long> ids = entrenamientos.stream().map(Entrenamiento::getId).toList();
        Map<Long, Integer> xp = ids.isEmpty() ? Map.of() : movimientoRepository.xpDeEntrenamientos(ids).stream()
                .collect(Collectors.toMap(IdValor::getId, f -> f.getValor().intValue()));
        Map<Long, String> evidencias = evidenciaService.estados(ids);
        return entrenamientos.stream().map(e -> dto(e, xp.get(e.getId()), evidencias.get(e.getId()))).toList();
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
