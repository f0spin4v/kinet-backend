package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.matchmaking.EnfrentamientoDTO;
import co.edu.uniquindio.kinet.dto.matchmaking.ParticipanteDTO;
import co.edu.uniquindio.kinet.dto.matchmaking.SolicitudDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.*;
import co.edu.uniquindio.kinet.repositories.EnfrentamientoRepository;
import co.edu.uniquindio.kinet.repositories.MovimientoXpRepository;
import co.edu.uniquindio.kinet.repositories.SolicitudMatchmakingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Matchmaking y enfrentamientos de 24 horas. El primer enfrentamiento del jugador en la temporada es una
 * COLOCACION individual; los siguientes se emparejan con el rival en cola de rango más parecido.
 * El puntaje es el XP de los entrenamientos que empezaron dentro de la ventana.
 */
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final SolicitudMatchmakingRepository solicitudRepository;
    private final EnfrentamientoRepository enfrentamientoRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final TemporadaService temporadaService;
    private final UsuarioService usuarioService;
    private final LogroService logroService;
    private final NotificacionService notificacionService;

    @Transactional
    public SolicitudDTO solicitar(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        finalizarVencidos();
        Temporada temporada = temporadaService.activaObligatoria();
        ParticipacionTemporada participacion = temporadaService.inscribir(usuario, temporada);
        temporadaService.exigirActiva(participacion);

        if (enfrentamientoRepository.existeDeUsuarioEnEstado(usuario.getId(), EstadoEnfrentamiento.EN_CURSO)) {
            throw new ConflictoException("Ya tienes un enfrentamiento en curso.");
        }
        if (solicitudRepository.findFirstByUsuarioIdAndTemporadaIdAndEstado(usuario.getId(), temporada.getId(),
                EstadoSolicitud.EN_COLA).isPresent()) {
            throw new ConflictoException("Ya estás en la cola de matchmaking.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudMatchmaking solicitud = SolicitudMatchmaking.builder()
                .usuario(usuario).temporada(temporada).fechaSolicitud(ahora).build();

        // primera vez en la temporada: colocación individual
        if (enfrentamientoRepository.contarDeUsuarioEnTemporada(usuario.getId(), temporada.getId(),
                EstadoEnfrentamiento.CANCELADO) == 0) {
            Enfrentamiento colocacion = crearEnfrentamiento(temporada, TipoEnfrentamiento.COLOCACION, ahora, usuario);
            emparejar(solicitud, colocacion, ahora);
            notificacionService.notificar(usuario, TipoNotificacion.MATCHMAKING, "Colocación iniciada",
                    "Tu enfrentamiento de colocación dura " + ReglasJuego.HORAS_ENFRENTAMIENTO
                            + " horas: entrena para sumar puntos.");
            return dto(solicitud, colocacion, "Empezó tu colocación. Suma XP entrenando durante las próximas 24 horas.");
        }

        Optional<SolicitudMatchmaking> rival = buscarRival(usuario, temporada, participacion);
        if (rival.isEmpty()) {
            solicitudRepository.save(solicitud);
            return dto(solicitud, null, "Estás en la cola. Te avisaremos cuando encontremos rival.");
        }
        Usuario otro = rival.get().getUsuario();
        Enfrentamiento duelo = crearEnfrentamiento(temporada, TipoEnfrentamiento.ESTANDAR, ahora, usuario, otro);
        emparejar(solicitud, duelo, ahora);
        emparejar(rival.get(), duelo, ahora);
        Map<Long, String> tags = usuarioService.gamertags(List.of(usuario.getId(), otro.getId()));
        String tagUsuario = tags.get(usuario.getId());
        String tagRival = tags.get(otro.getId());
        notificacionService.notificar(usuario, TipoNotificacion.MATCHMAKING, "Rival encontrado",
                "Te enfrentas a " + tagRival + " durante " + ReglasJuego.HORAS_ENFRENTAMIENTO + " horas.");
        notificacionService.notificar(otro, TipoNotificacion.MATCHMAKING, "Rival encontrado",
                "Te enfrentas a " + tagUsuario + " durante " + ReglasJuego.HORAS_ENFRENTAMIENTO + " horas.");
        return dto(solicitud, duelo, "¡Rival encontrado: " + tagRival + "!");
    }

    @Transactional
    public void cancelar(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Temporada temporada = temporadaService.activaObligatoria();
        SolicitudMatchmaking solicitud = solicitudRepository.findFirstByUsuarioIdAndTemporadaIdAndEstado(
                        usuario.getId(), temporada.getId(), EstadoSolicitud.EN_COLA)
                .orElseThrow(() -> new NoEncontradoException("No estás en la cola de matchmaking."));
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitudRepository.save(solicitud);
    }

    @Transactional
    public PaginaDTO<EnfrentamientoDTO> misEnfrentamientos(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        finalizarVencidos();
        // primero los ids de la página (con su orden); luego los enfrentamientos con sus participantes en una consulta
        return PaginaDTO.de(enfrentamientoRepository.idsDeUsuario(usuario.getId(), pagina), orden -> {
            if (orden.isEmpty()) {
                return List.of();
            }
            Map<Long, Enfrentamiento> porId = enfrentamientoRepository.conParticipantes(orden).stream()
                    .collect(Collectors.toMap(Enfrentamiento::getId, e -> e));
            return dtos(orden.stream().map(porId::get).toList());
        });
    }

    @Transactional
    public EnfrentamientoDTO detalle(String correo, Long idEnfrentamiento) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        finalizarVencidos();
        return enfrentamientoRepository.deUsuarioPorId(idEnfrentamiento, usuario.getId())
                .map(e -> dtos(List.of(e)).get(0))
                .orElseThrow(() -> new NoEncontradoException("El enfrentamiento no existe."));
    }

    /** Cierra los enfrentamientos cuya ventana ya terminó. Lo llama la tarea programada y cada consulta. */
    @Transactional
    public int finalizarVencidos() {
        List<Enfrentamiento> vencidos = enfrentamientoRepository.findByEstadoAndFechaFinLessThanEqual(
                EstadoEnfrentamiento.EN_CURSO, LocalDateTime.now());
        vencidos.forEach(this::finalizar);
        return vencidos.size();
    }

    /** Saca al jugador de la cola (si estaba). Lo usan el abandono y la descalificación. */
    @Transactional
    public void salirDeCola(Usuario usuario, Temporada temporada) {
        solicitudRepository.cambiarEstadoDeUsuario(temporada.getId(), usuario.getId(), EstadoSolicitud.EN_COLA,
                EstadoSolicitud.CANCELADA);
    }

    /** Al cerrar la temporada: los enfrentamientos en curso se cancelan (sin puntos) y la cola se vacía. */
    @Transactional
    public void cancelarDeTemporada(Temporada temporada) {
        enfrentamientoRepository.cambiarEstadoDeTemporada(temporada.getId(), EstadoEnfrentamiento.EN_CURSO,
                EstadoEnfrentamiento.CANCELADO);
        solicitudRepository.cambiarEstadoDeTemporada(temporada.getId(), EstadoSolicitud.EN_COLA, EstadoSolicitud.CANCELADA);
    }

    // ---------- reglas ----------

    /**
     * COLOCACION: puntos = min(XP, 300) / 3. ESTANDAR: más XP gana (+25), pierde (-15, sin bajar de 0),
     * empate (+10 cada uno).
     */
    private void finalizar(Enfrentamiento enfrentamiento) {
        List<ParticipanteEnfrentamiento> participantes = enfrentamiento.getParticipantes();
        Map<Long, Long> xp = xpEnVentana(enfrentamiento);
        participantes.forEach(p -> p.setPuntaje(BigDecimal.valueOf(xp.getOrDefault(p.getUsuario().getId(), 0L))));

        if (enfrentamiento.getTipo() == TipoEnfrentamiento.COLOCACION) {
            ParticipanteEnfrentamiento p = participantes.get(0);
            int puntos = ReglasJuego.puntosColocacion(p.getPuntaje().longValue());
            p.setResultado(ResultadoEnfrentamiento.COMPLETADO);
            sumar(enfrentamiento, p, puntos, "Terminó tu colocación: +" + puntos + " puntos de temporada.");
        } else {
            ParticipanteEnfrentamiento a = participantes.get(0);
            ParticipanteEnfrentamiento b = participantes.get(1);
            int comparacion = a.getPuntaje().compareTo(b.getPuntaje());
            if (comparacion == 0) {
                resultado(enfrentamiento, a, ResultadoEnfrentamiento.EMPATE, ReglasJuego.PUNTOS_EMPATE);
                resultado(enfrentamiento, b, ResultadoEnfrentamiento.EMPATE, ReglasJuego.PUNTOS_EMPATE);
            } else {
                ParticipanteEnfrentamiento ganador = comparacion > 0 ? a : b;
                ParticipanteEnfrentamiento perdedor = comparacion > 0 ? b : a;
                resultado(enfrentamiento, ganador, ResultadoEnfrentamiento.VICTORIA, ReglasJuego.PUNTOS_VICTORIA);
                resultado(enfrentamiento, perdedor, ResultadoEnfrentamiento.DERROTA, ReglasJuego.PUNTOS_DERROTA);
            }
        }
        enfrentamiento.setEstado(EstadoEnfrentamiento.FINALIZADO);
        enfrentamientoRepository.save(enfrentamiento);
        participantes.forEach(p -> logroService.evaluar(p.getUsuario()));
    }

    private void resultado(Enfrentamiento enfrentamiento, ParticipanteEnfrentamiento p, ResultadoEnfrentamiento resultado,
                           int puntos) {
        p.setResultado(resultado);
        sumar(enfrentamiento, p, puntos, "Resultado: " + resultado.name() + " ("
                + p.getPuntaje().intValue() + " XP). " + (puntos >= 0 ? "+" : "") + puntos + " puntos de temporada.");
    }

    private void sumar(Enfrentamiento enfrentamiento, ParticipanteEnfrentamiento p, int puntos, String mensaje) {
        ParticipacionTemporada participacion = temporadaService
                .participacion(p.getUsuario().getId(), enfrentamiento.getTemporada().getId())
                .orElseGet(() -> temporadaService.inscribir(p.getUsuario(), enfrentamiento.getTemporada()));
        temporadaService.sumarPuntos(participacion, puntos);
        notificacionService.notificar(p.getUsuario(), TipoNotificacion.MATCHMAKING, "Enfrentamiento terminado", mensaje);
    }

    /** El rival en cola de rango más parecido; en empate, el que lleva más tiempo esperando (una consulta). */
    private Optional<SolicitudMatchmaking> buscarRival(Usuario usuario, Temporada temporada,
                                                        ParticipacionTemporada propia) {
        return solicitudRepository.rivales(temporada.getId(), usuario.getId(), propia.getRangoActual().getOrden(),
                        EstadoSolicitud.EN_COLA, EstadoParticipacionTemporada.ACTIVA, EstadoEnfrentamiento.EN_CURSO,
                        PageRequest.of(0, 1))
                .stream().findFirst();
    }

    private Enfrentamiento crearEnfrentamiento(Temporada temporada, TipoEnfrentamiento tipo, LocalDateTime inicio,
                                               Usuario... jugadores) {
        Enfrentamiento enfrentamiento = Enfrentamiento.builder()
                .temporada(temporada)
                .tipo(tipo)
                .estado(EstadoEnfrentamiento.EN_CURSO)
                .fechaInicio(inicio)
                .fechaFin(inicio.plusHours(ReglasJuego.HORAS_ENFRENTAMIENTO))
                .build();
        for (Usuario jugador : jugadores) {
            enfrentamiento.getParticipantes().add(ParticipanteEnfrentamiento.builder()
                    .id(new ParticipanteEnfrentamiento.Id())
                    .enfrentamiento(enfrentamiento)
                    .usuario(jugador)
                    .build());
        }
        return enfrentamientoRepository.save(enfrentamiento);
    }

    private void emparejar(SolicitudMatchmaking solicitud, Enfrentamiento enfrentamiento, LocalDateTime ahora) {
        solicitud.setEnfrentamiento(enfrentamiento);
        solicitud.setEstado(EstadoSolicitud.EMPAREJADA);
        solicitud.setFechaEmparejamiento(ahora);
        solicitudRepository.save(solicitud);
    }

    /** XP de cada participante dentro de la ventana del enfrentamiento (una consulta para todos). */
    private Map<Long, Long> xpEnVentana(Enfrentamiento e) {
        return movimientoRepository.xpEnVentana(idsDe(e), e.getFechaInicio(), e.getFechaFin(), null);
    }

    private static List<Long> idsDe(Enfrentamiento e) {
        return e.getParticipantes().stream().map(p -> p.getUsuario().getId()).toList();
    }

    /** Arma los DTO con los gamertags de todos los participantes en una sola consulta. */
    private List<EnfrentamientoDTO> dtos(List<Enfrentamiento> enfrentamientos) {
        Map<Long, String> gamertags = usuarioService.gamertags(enfrentamientos.stream()
                .flatMap(e -> idsDe(e).stream()).toList());
        return enfrentamientos.stream().map(e -> {
            Map<Long, Long> enVivo = e.getEstado() == EstadoEnfrentamiento.EN_CURSO ? xpEnVentana(e) : Map.of();
            List<ParticipanteDTO> participantes = e.getParticipantes().stream()
                    .map(p -> new ParticipanteDTO(p.getUsuario().getId(), gamertags.get(p.getUsuario().getId()),
                            e.getEstado() == EstadoEnfrentamiento.EN_CURSO
                                    ? BigDecimal.valueOf(enVivo.getOrDefault(p.getUsuario().getId(), 0L))
                                    : p.getPuntaje(),
                            p.getResultado() == null ? null : p.getResultado().name()))
                    .toList();
            return new EnfrentamientoDTO(e.getId(), e.getTemporada().getNombre(), e.getTipo().name(),
                    e.getEstado().name(), e.getFechaInicio(), e.getFechaFin(), participantes);
        }).toList();
    }

    private SolicitudDTO dto(SolicitudMatchmaking s, Enfrentamiento e, String mensaje) {
        return new SolicitudDTO(s.getId(), s.getEstado().name(), s.getFechaSolicitud(),
                e == null ? null : dtos(List.of(e)).get(0), mensaje);
    }
}
