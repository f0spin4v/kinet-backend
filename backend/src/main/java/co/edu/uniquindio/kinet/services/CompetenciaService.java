package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.competencia.CompetenciaDTO;
import co.edu.uniquindio.kinet.dto.competencia.CrearCompetenciaDTO;
import co.edu.uniquindio.kinet.dto.competencia.ParticipanteCompetenciaDTO;
import co.edu.uniquindio.kinet.exceptions.AccesoDenegadoException;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.*;
import co.edu.uniquindio.kinet.repositories.CompetenciaRepository;
import co.edu.uniquindio.kinet.repositories.MovimientoXpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Competencias amistosas por invitación. No dan puntos de temporada: el ganador recibe XP.
 * <ul>
 *   <li>DUELO: 1 invitado. Empieza cuando acepta; si rechaza (o no responde en 24 horas) se cancela.</li>
 *   <li>GRUPAL: 2 a 9 invitados, una sola ronda. Empieza a la hora fijada si aceptaron al menos 2 jugadores
 *   (contando al creador).</li>
 *   <li>TORNEO: 3 a 15 invitados, rondas eliminatorias: en cada ronda pasa la mitad con más XP (redondeando
 *   hacia arriba; en empate, quien aceptó antes) hasta que quedan 2, que se juegan la final como un duelo.
 *   Necesita al menos 3 jugadores.</li>
 * </ul>
 * El puntaje de una ronda es el XP de los entrenamientos que empezaron dentro de ella (solo de la actividad
 * elegida, si hay una). Las invitaciones sin responder vencen al empezar. Si el mejor puntaje es 0, nadie gana.
 * Un empate en el primer lugar reparte el premio a la mitad.
 */
@Service
@RequiredArgsConstructor
public class CompetenciaService {

    private static final List<EstadoCompetencia> VIGENTES = List.of(EstadoCompetencia.PENDIENTE, EstadoCompetencia.EN_CURSO);

    private final CompetenciaRepository competenciaRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final UsuarioService usuarioService;
    private final CatalogoService catalogoService;
    private final ProgresoService progresoService;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    @Transactional
    public CompetenciaDTO crear(String correo, CrearCompetenciaDTO dto) {
        Usuario creador = usuarioService.obtenerEntidad(correo);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Set<Long> ids = new LinkedHashSet<>(dto.idsInvitados());
        if (ids.contains(creador.getId())) {
            throw new ReglaNegocioException("No puedes invitarte a ti mismo.");
        }
        validarCantidad(dto.tipo(), ids.size());
        exigirCupo(creador);

        LocalDateTime inicio = null;
        if (dto.tipo() != TipoCompetencia.DUELO) {
            if (dto.fechaInicio() == null) {
                throw new ReglaNegocioException("Indica cuándo empieza la competencia.");
            }
            if (!dto.fechaInicio().isAfter(ahora)) {
                throw new ReglaNegocioException("La competencia tiene que empezar en el futuro.");
            }
            if (dto.fechaInicio().isAfter(ahora.plusDays(ReglasJuego.DIAS_MAXIMOS_PARA_INICIAR))) {
                throw new ReglaNegocioException("La competencia tiene que empezar dentro de los próximos "
                        + ReglasJuego.DIAS_MAXIMOS_PARA_INICIAR + " días.");
            }
            inicio = dto.fechaInicio();
        }
        Competencia competencia = Competencia.builder()
                .creador(creador)
                .tipo(dto.tipo())
                .nombre(dto.nombre().trim())
                .tipoActividad(dto.idTipoActividad() == null ? null : catalogoService.actividadActiva(dto.idTipoActividad()))
                .duracionRondaHoras(dto.duracionRondaHoras())
                .fechaCreacion(ahora)
                .fechaInicio(inicio)
                .build();
        competencia.getParticipantes().add(participante(competencia, creador, ahora, EstadoInvitacion.ACEPTADO));
        List<Usuario> invitados = usuarioService.obtenerVarios(ids);
        for (Usuario invitado : invitados) {
            if (!invitado.tieneRol(Rol.JUGADOR) || invitado.getEstado() != EstadoUsuario.ACTIVO) {
                throw new ReglaNegocioException("Solo puedes invitar a jugadores con la cuenta activa.");
            }
            competencia.getParticipantes().add(participante(competencia, invitado, ahora, EstadoInvitacion.INVITADO));
        }
        competencia = competenciaRepository.save(competencia);

        String tag = gamertag(creador);
        for (Usuario invitado : invitados) {
            notificacionService.notificar(invitado, TipoNotificacion.COMPETENCIA, "Te invitaron a competir",
                    tag + " te invitó a " + nombreTipo(competencia) + " \"" + competencia.getNombre() + "\".");
        }
        return dto(competencia);
    }

    /** Acepta la invitación. Un duelo empieza en ese momento. */
    @Transactional
    public CompetenciaDTO aceptar(String correo, Long idCompetencia) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        Competencia competencia = buscar(idCompetencia, usuario);
        ParticipanteCompetencia invitacion = invitacionPendiente(competencia, usuario);
        exigirCupo(usuario);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        invitacion.setEstadoInvitacion(EstadoInvitacion.ACEPTADO);
        invitacion.setFechaRespuesta(ahora);
        if (competencia.getTipo() == TipoCompetencia.DUELO) {
            competencia.setFechaInicio(ahora);
            iniciar(competencia);
        }
        competencia = competenciaRepository.save(competencia);
        notificacionService.notificar(competencia.getCreador(), TipoNotificacion.COMPETENCIA, "Invitación aceptada",
                gamertag(usuario) + " aceptó \"" + competencia.getNombre() + "\"."
                        + (competencia.getEstado() == EstadoCompetencia.EN_CURSO ? " ¡Ya empezó!" : ""));
        return dto(competencia);
    }

    /** Rechaza la invitación. Un duelo rechazado se cancela. */
    @Transactional
    public CompetenciaDTO rechazar(String correo, Long idCompetencia) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        Competencia competencia = buscar(idCompetencia, usuario);
        ParticipanteCompetencia invitacion = invitacionPendiente(competencia, usuario);
        invitacion.setEstadoInvitacion(EstadoInvitacion.RECHAZADO);
        invitacion.setFechaRespuesta(LocalDateTime.now(reloj));
        if (competencia.getTipo() == TipoCompetencia.DUELO) {
            competencia.setEstado(EstadoCompetencia.CANCELADA);
        }
        competencia = competenciaRepository.save(competencia);
        notificacionService.notificar(competencia.getCreador(), TipoNotificacion.COMPETENCIA, "Invitación rechazada",
                gamertag(usuario) + " rechazó \"" + competencia.getNombre() + "\".");
        return dto(competencia);
    }

    /** Solo el creador, y solo mientras no haya empezado. */
    @Transactional
    public CompetenciaDTO cancelar(String correo, Long idCompetencia) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        Competencia competencia = buscar(idCompetencia, usuario);
        if (!competencia.getCreador().getId().equals(usuario.getId())) {
            throw new AccesoDenegadoException("Solo quien creó la competencia puede cancelarla.");
        }
        if (competencia.getEstado() != EstadoCompetencia.PENDIENTE) {
            throw new ReglaNegocioException("Solo se puede cancelar una competencia que no ha empezado.");
        }
        competencia.setEstado(EstadoCompetencia.CANCELADA);
        competencia = competenciaRepository.save(competencia);
        avisar(competencia, usuario.getId(), "Competencia cancelada",
                "\"" + competencia.getNombre() + "\" se canceló.");
        return dto(competencia);
    }

    /** Invitaciones que el jugador todavía puede aceptar o rechazar. */
    @Transactional
    public PaginaDTO<CompetenciaDTO> invitaciones(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        return pagina(competenciaRepository.idsDeUsuario(usuario.getId(), EstadoInvitacion.INVITADO,
                List.of(EstadoCompetencia.PENDIENTE), pagina));
    }

    /** Competencias en las que el jugador participa o participó (sin las invitaciones que rechazó o dejó vencer). */
    @Transactional
    public PaginaDTO<CompetenciaDTO> misCompetencias(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        return pagina(competenciaRepository.idsDeUsuario(usuario.getId(), EstadoInvitacion.ACEPTADO,
                EnumSet.allOf(EstadoCompetencia.class), pagina));
    }

    /** Una página de ids (ya ordenada) pasa a DTO: las competencias y sus participantes se cargan en una consulta. */
    private PaginaDTO<CompetenciaDTO> pagina(Page<Long> ids) {
        return PaginaDTO.de(ids, orden -> {
            if (orden.isEmpty()) {
                return List.of();
            }
            Map<Long, Competencia> porId = competenciaRepository.conParticipantes(orden).stream()
                    .collect(Collectors.toMap(Competencia::getId, c -> c));
            return dtos(orden.stream().map(porId::get).toList());
        });
    }

    @Transactional
    public CompetenciaDTO detalle(String correo, Long idCompetencia) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        procesar();
        return dto(buscar(idCompetencia, usuario));
    }

    /**
     * Avanza las competencias: vence los duelos sin respuesta, empieza las que llegaron a su hora y cierra las
     * rondas que terminaron. Lo llaman la tarea programada y cada consulta. Devuelve cuántas cambiaron.
     */
    @Transactional
    public int procesar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int cambios = 0;
        for (Competencia duelo : competenciaRepository.findByTipoAndEstadoAndFechaCreacionLessThanEqual(
                TipoCompetencia.DUELO, EstadoCompetencia.PENDIENTE, ahora.minusHours(ReglasJuego.HORAS_RESPUESTA_DUELO))) {
            vencerInvitaciones(duelo, ahora);
            duelo.setEstado(EstadoCompetencia.CANCELADA);
            competenciaRepository.save(duelo);
            avisar(duelo, null, "Duelo cancelado", "Nadie respondió a \"" + duelo.getNombre() + "\" a tiempo.");
            cambios++;
        }
        for (Competencia c : competenciaRepository.findByEstadoAndFechaInicioLessThanEqual(EstadoCompetencia.PENDIENTE, ahora)) {
            vencerInvitaciones(c, ahora);
            if (aceptados(c).size() >= minimoParaEmpezar(c.getTipo())) {
                iniciar(c);
                avisar(c, null, "¡Empezó la competencia!", "\"" + c.getNombre() + "\" está en curso: entrena para sumar XP.");
            } else {
                c.setEstado(EstadoCompetencia.CANCELADA);
                avisar(c, null, "Competencia cancelada",
                        "\"" + c.getNombre() + "\" no se jugó: no aceptaron suficientes jugadores.");
            }
            competenciaRepository.save(c);
            cambios++;
        }
        for (Competencia c : competenciaRepository.findByEstadoAndFechaFinLessThanEqual(EstadoCompetencia.EN_CURSO, ahora)) {
            while (c.getEstado() == EstadoCompetencia.EN_CURSO && !c.getFechaFin().isAfter(ahora)) {
                cerrarRonda(c);
            }
            competenciaRepository.save(c);
            cambios++;
        }
        return cambios;
    }

    // ---------- reglas ----------

    private void iniciar(Competencia c) {
        c.setEstado(EstadoCompetencia.EN_CURSO);
        c.setRondaActual(1);
        c.setFechaFin(c.getFechaInicio().plusHours(c.getDuracionRondaHoras()));
    }

    /** Suma el XP de la ronda a cada jugador vivo; en torneo elimina a la mitad o decide la final. */
    private void cerrarRonda(Competencia c) {
        List<ParticipanteCompetencia> vivos = vivos(c);
        Map<Long, Long> xpPorUsuario = xpEnRonda(c, vivos);
        Map<ParticipanteCompetencia, Long> xpRonda = new HashMap<>();
        for (ParticipanteCompetencia p : vivos) {
            long xp = xpPorUsuario.getOrDefault(p.getUsuario().getId(), 0L);
            xpRonda.put(p, xp);
            p.setPuntaje(p.getPuntaje().add(BigDecimal.valueOf(xp)));
        }
        if (c.getTipo() != TipoCompetencia.TORNEO) {
            finalizar(c, vivos, p -> p.getPuntaje().longValue());
            return;
        }
        if (vivos.size() <= 2) {
            finalizar(c, vivos, xpRonda::get);
            return;
        }
        List<ParticipanteCompetencia> orden = new ArrayList<>(vivos);
        orden.sort(Comparator.comparing((ParticipanteCompetencia p) -> xpRonda.get(p)).reversed()
                .thenComparing(ParticipanteCompetencia::getFechaRespuesta));
        int pasan = (orden.size() + 1) / 2;
        for (int i = pasan; i < orden.size(); i++) {
            orden.get(i).setEliminadoEnRonda(c.getRondaActual());
            orden.get(i).setResultado(ResultadoCompetencia.PERDEDOR);
            notificacionService.notificar(orden.get(i).getUsuario(), TipoNotificacion.COMPETENCIA, "Quedaste fuera",
                    "Saliste de \"" + c.getNombre() + "\" en la ronda " + c.getRondaActual() + ".");
        }
        c.setRondaActual(c.getRondaActual() + 1);
        c.setFechaFin(c.getFechaFin().plusHours(c.getDuracionRondaHoras()));
    }

    /**
     * Decide entre los que llegaron al final según "puntos": el mejor gana; empate en el primer lugar = EMPATE
     * con medio premio; si el mejor tiene 0, todos empatan y no hay premio. Luego asigna las posiciones.
     */
    private void finalizar(Competencia c, List<ParticipanteCompetencia> finalistas,
                           java.util.function.Function<ParticipanteCompetencia, Long> puntos) {
        long mejor = finalistas.stream().mapToLong(puntos::apply).max().orElse(0);
        List<ParticipanteCompetencia> primeros = finalistas.stream().filter(p -> puntos.apply(p) == mejor).toList();
        int premio = mejor == 0 ? 0 : primeros.size() == 1 ? premio(c.getTipo()) : premio(c.getTipo()) / 2;
        for (ParticipanteCompetencia p : finalistas) {
            p.setResultado(mejor == 0 ? ResultadoCompetencia.EMPATE
                    : !primeros.contains(p) ? ResultadoCompetencia.PERDEDOR
                    : primeros.size() == 1 ? ResultadoCompetencia.GANADOR : ResultadoCompetencia.EMPATE);
        }
        // posiciones: primero quien llegó más lejos, luego por puntaje (los empatados comparten lugar)
        List<ParticipanteCompetencia> todos = new ArrayList<>(aceptados(c));
        Comparator<ParticipanteCompetencia> orden = Comparator
                .comparing((ParticipanteCompetencia p) -> p.getEliminadoEnRonda() == null ? Integer.MAX_VALUE
                        : p.getEliminadoEnRonda()).reversed()
                .thenComparing(p -> finalistas.contains(p) ? puntos.apply(p) : p.getPuntaje().longValue(),
                        Comparator.reverseOrder());
        todos.sort(orden);
        for (int i = 0; i < todos.size(); i++) {
            ParticipanteCompetencia p = todos.get(i);
            boolean empataConAnterior = i > 0 && orden.compare(todos.get(i - 1), p) == 0;
            p.setPosicion(empataConAnterior ? todos.get(i - 1).getPosicion() : i + 1);
        }
        c.setEstado(EstadoCompetencia.FINALIZADA);
        for (ParticipanteCompetencia p : todos) {
            boolean premiado = premio > 0 && primeros.contains(p);
            if (premiado) {
                progresoService.porCompetencia(p.getUsuario(), c, premio);
            }
            notificacionService.notificar(p.getUsuario(), TipoNotificacion.COMPETENCIA, "Terminó \"" + c.getNombre() + "\"",
                    "Quedaste en la posición " + p.getPosicion() + " (" + p.getResultado().name() + ")."
                            + (premiado ? " +" + premio + " XP." : ""));
        }
    }

    /** XP de la ronda en curso [fin - duración, fin) de cada jugador, en una sola consulta. */
    private Map<Long, Long> xpEnRonda(Competencia c, List<ParticipanteCompetencia> jugadores) {
        LocalDateTime hasta = c.getFechaFin();
        return movimientoRepository.xpEnVentana(jugadores.stream().map(p -> p.getUsuario().getId()).toList(),
                hasta.minusHours(c.getDuracionRondaHoras()), hasta,
                c.getTipoActividad() == null ? null : c.getTipoActividad().getId());
    }

    private void vencerInvitaciones(Competencia c, LocalDateTime ahora) {
        c.getParticipantes().stream()
                .filter(p -> p.getEstadoInvitacion() == EstadoInvitacion.INVITADO)
                .forEach(p -> {
                    p.setEstadoInvitacion(EstadoInvitacion.VENCIDA);
                    p.setFechaRespuesta(ahora);
                });
    }

    private void exigirCupo(Usuario usuario) {
        if (competenciaRepository.contarActivasDe(usuario.getId(), EstadoInvitacion.ACEPTADO, VIGENTES)
                >= ReglasJuego.MAX_COMPETENCIAS_ACTIVAS) {
            throw new ConflictoException("Ya tienes " + ReglasJuego.MAX_COMPETENCIAS_ACTIVAS
                    + " competencias pendientes o en curso: termina alguna primero.");
        }
    }

    private static void validarCantidad(TipoCompetencia tipo, int invitados) {
        switch (tipo) {
            case DUELO -> {
                if (invitados != 1) throw new ReglaNegocioException("Un duelo es contra un solo jugador.");
            }
            case GRUPAL -> {
                if (invitados < 2 || invitados > 9)
                    throw new ReglaNegocioException("Una competencia grupal invita de 2 a 9 jugadores.");
            }
            case TORNEO -> {
                if (invitados < 3 || invitados > 15)
                    throw new ReglaNegocioException("Un torneo invita de 3 a 15 jugadores.");
            }
        }
    }

    private static int minimoParaEmpezar(TipoCompetencia tipo) {
        return tipo == TipoCompetencia.TORNEO ? 3 : 2;
    }

    private static int premio(TipoCompetencia tipo) {
        return switch (tipo) {
            case DUELO -> ReglasJuego.PREMIO_DUELO;
            case GRUPAL -> ReglasJuego.PREMIO_GRUPAL;
            case TORNEO -> ReglasJuego.PREMIO_TORNEO;
        };
    }

    // ---------- apoyo ----------

    private Competencia buscar(Long idCompetencia, Usuario usuario) {
        Competencia competencia = competenciaRepository.findById(idCompetencia)
                .orElseThrow(() -> new NoEncontradoException("La competencia no existe."));
        if (estadoDe(competencia, usuario.getId()) == null) {
            throw new NoEncontradoException("La competencia no existe.");
        }
        return competencia;
    }

    private ParticipanteCompetencia invitacionPendiente(Competencia c, Usuario usuario) {
        ParticipanteCompetencia p = c.getParticipantes().stream()
                .filter(x -> x.getUsuario().getId().equals(usuario.getId())).findFirst().orElseThrow();
        if (c.getEstado() != EstadoCompetencia.PENDIENTE || p.getEstadoInvitacion() != EstadoInvitacion.INVITADO) {
            throw new ConflictoException("Esta invitación ya no está pendiente.");
        }
        return p;
    }

    private static ParticipanteCompetencia participante(Competencia c, Usuario u, LocalDateTime ahora,
                                                        EstadoInvitacion estado) {
        return ParticipanteCompetencia.builder()
                .id(new ParticipanteCompetencia.Id(null, u.getId()))
                .competencia(c)
                .usuario(u)
                .estadoInvitacion(estado)
                .fechaInvitacion(ahora)
                .fechaRespuesta(estado == EstadoInvitacion.ACEPTADO ? ahora : null)
                .build();
    }

    private static EstadoInvitacion estadoDe(Competencia c, Long idUsuario) {
        return c.getParticipantes().stream().filter(p -> p.getUsuario().getId().equals(idUsuario))
                .map(ParticipanteCompetencia::getEstadoInvitacion).findFirst().orElse(null);
    }

    private static List<ParticipanteCompetencia> aceptados(Competencia c) {
        return c.getParticipantes().stream()
                .filter(p -> p.getEstadoInvitacion() == EstadoInvitacion.ACEPTADO).toList();
    }

    private static List<ParticipanteCompetencia> vivos(Competencia c) {
        return aceptados(c).stream().filter(p -> p.getEliminadoEnRonda() == null).toList();
    }

    /** Aviso a todos los que aceptaron, menos a "excepto" (quien hizo la acción). */
    private void avisar(Competencia c, Long excepto, String titulo, String mensaje) {
        aceptados(c).stream()
                .filter(p -> !p.getUsuario().getId().equals(excepto))
                .forEach(p -> notificacionService.notificar(p.getUsuario(), TipoNotificacion.COMPETENCIA, titulo, mensaje));
    }

    private String gamertag(Usuario usuario) {
        return usuarioService.perfilDe(usuario).getGamertag();
    }

    private static String nombreTipo(Competencia c) {
        return switch (c.getTipo()) {
            case DUELO -> "un duelo";
            case GRUPAL -> "una competencia grupal";
            case TORNEO -> "un torneo";
        };
    }

    private CompetenciaDTO dto(Competencia c) {
        return dtos(List.of(c)).get(0);
    }

    /**
     * Arma los DTO con los gamertags de todos los participantes en una sola consulta. Mientras una competencia
     * está en curso, el puntaje de los que siguen vivos incluye lo que llevan de la ronda actual.
     */
    private List<CompetenciaDTO> dtos(List<Competencia> competencias) {
        Map<Long, String> gamertags = usuarioService.gamertags(competencias.stream()
                .flatMap(c -> c.getParticipantes().stream().map(p -> p.getUsuario().getId()))
                .toList());
        return competencias.stream().map(c -> {
            boolean enCurso = c.getEstado() == EstadoCompetencia.EN_CURSO;
            Map<Long, Long> enVivo = enCurso ? xpEnRonda(c, vivos(c)) : Map.of();
            List<ParticipanteCompetenciaDTO> participantes = c.getParticipantes().stream()
                    .map(p -> new ParticipanteCompetenciaDTO(p.getUsuario().getId(), gamertags.get(p.getUsuario().getId()),
                            p.getEstadoInvitacion().name(), p.getFechaRespuesta(),
                            p.getPuntaje().add(BigDecimal.valueOf(enVivo.getOrDefault(p.getUsuario().getId(), 0L)))
                                    .setScale(2),
                            p.getEliminadoEnRonda(), p.getPosicion(),
                            p.getResultado() == null ? null : p.getResultado().name()))
                    .toList();
            return new CompetenciaDTO(c.getId(), c.getTipo().name(), c.getNombre(), c.getEstado().name(),
                    c.getCreador().getId(), gamertags.get(c.getCreador().getId()),
                    c.getTipoActividad() == null ? null : c.getTipoActividad().getNombre(), c.getDuracionRondaHoras(),
                    c.getRondaActual(), premio(c.getTipo()), c.getFechaCreacion(), c.getFechaInicio(), c.getFechaFin(),
                    participantes);
        }).toList();
    }
}
