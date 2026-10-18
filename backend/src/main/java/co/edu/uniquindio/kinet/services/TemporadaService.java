package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.catalogo.RangoDTO;
import co.edu.uniquindio.kinet.dto.temporada.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.CausaCambioRango;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacionTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Temporadas competitivas, inscripción, puntos y rango. Los puntos de temporada salen de los enfrentamientos
 * (no del XP). Al empezar una temporada nueva se conserva un porcentaje de los puntos finales de la anterior,
 * y los cupos de ascenso y descenso de cada liga suben o bajan ese punto de partida.
 * Solo la participación ACTIVA suma puntos y aparece en el ranking; cada cambio de rango queda en HISTORIAL_RANGO.
 */
@Service
@RequiredArgsConstructor
public class TemporadaService {

    private final TemporadaRepository temporadaRepository;
    private final ParticipacionTemporadaRepository participacionRepository;
    private final HistorialRangoRepository historialRepository;
    private final LigaRepository ligaRepository;
    private final RangoRepository rangoRepository;
    private static final List<EstadoParticipacionTemporada> EN_RANKING =
            List.of(EstadoParticipacionTemporada.ACTIVA, EstadoParticipacionTemporada.FINALIZADA);

    private final CatalogoService catalogoService;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public Optional<Temporada> temporadaActiva() {
        return temporadaRepository.findFirstByEstado(EstadoTemporada.ACTIVA);
    }

    @Transactional(readOnly = true)
    public Temporada activaObligatoria() {
        return temporadaActiva().orElseThrow(() -> new NoEncontradoException("No hay una temporada activa."));
    }

    @Transactional(readOnly = true)
    public TemporadaDTO activa() {
        return TemporadaDTO.de(activaObligatoria());
    }

    /** Ranking de la temporada activa (los primeros "limite" jugadores), limitado en la base de datos. */
    @Transactional(readOnly = true)
    public List<PosicionRankingDTO> ranking(int limite) {
        Temporada temporada = activaObligatoria();
        if (limite <= 0) {
            return List.of();
        }
        List<ParticipacionTemporadaRepository.FilaRanking> filas =
                participacionRepository.ranking(temporada.getId(), EN_RANKING, PageRequest.of(0, limite));
        List<PosicionRankingDTO> ranking = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            ParticipacionTemporadaRepository.FilaRanking f = filas.get(i);
            String rango = f.getDivision() == null ? f.getLiga() : f.getLiga() + " " + f.getDivision();
            ranking.add(new PosicionRankingDTO(i + 1, f.getIdUsuario(), f.getGamertag(), f.getPuntos(), rango));
        }
        return ranking;
    }

    @Transactional(readOnly = true)
    public ParticipacionTemporadaDTO miParticipacion(String correo) {
        return dto(miParticipacionEntidad(correo));
    }

    /** Mis cambios de rango en la temporada activa, del más antiguo al más reciente. */
    @Transactional(readOnly = true)
    public PaginaDTO<HistorialRangoDTO> miHistorialRango(String correo, Pageable pagina) {
        ParticipacionTemporada p = miParticipacionEntidad(correo);
        return PaginaDTO.de(historialRepository
                .findByParticipacionUsuarioIdAndParticipacionTemporadaIdOrderByFechaCambioAscIdAsc(
                        p.getUsuario().getId(), p.getTemporada().getId(), pagina).map(HistorialRangoDTO::de));
    }

    @Transactional(readOnly = true)
    public ParticipacionTemporada miParticipacionEntidad(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Temporada temporada = activaObligatoria();
        return participacionRepository.findByUsuarioIdAndTemporadaId(usuario.getId(), temporada.getId())
                .orElseThrow(() -> new NoEncontradoException(
                        "Todavía no participas en la temporada: pide un enfrentamiento para inscribirte."));
    }

    /** El jugador se retira: deja de sumar puntos y sale del ranking. No puede volver en esta temporada. */
    @Transactional
    public ParticipacionTemporada abandonar(ParticipacionTemporada participacion) {
        exigirActiva(participacion);
        participacion.setEstado(EstadoParticipacionTemporada.ABANDONADA);
        return participacionRepository.save(participacion);
    }

    /** Moderación saca al jugador de la temporada: sin puntos finales ni posición. */
    @Transactional
    public ParticipacionTemporada descalificar(ParticipacionTemporada participacion, String motivo) {
        if (participacion.getEstado() == EstadoParticipacionTemporada.DESCALIFICADA) {
            throw new ConflictoException("El jugador ya está descalificado.");
        }
        participacion.setEstado(EstadoParticipacionTemporada.DESCALIFICADA);
        participacionRepository.save(participacion);
        notificacionService.notificar(participacion.getUsuario(), TipoNotificacion.FIN_TEMPORADA,
                "Descalificado de " + participacion.getTemporada().getNombre(), "Motivo: " + motivo.trim());
        return participacion;
    }

    /** Lanza un error si la participación no está ACTIVA (abandonó o fue descalificado). */
    public void exigirActiva(ParticipacionTemporada participacion) {
        switch (participacion.getEstado()) {
            case ACTIVA -> { }
            case ABANDONADA -> throw new ReglaNegocioException("Abandonaste esta temporada: espera a la siguiente.");
            case DESCALIFICADA -> throw new ReglaNegocioException("Fuiste descalificado de esta temporada.");
            case FINALIZADA -> throw new ReglaNegocioException("La temporada ya terminó.");
        }
    }

    /**
     * Inscribe al jugador en la temporada (si no lo estaba). Puntos iniciales = puntos finales de su última
     * temporada cerrada x porcentaje conservado de esa temporada, ajustados por ascenso o descenso de liga.
     */
    @Transactional
    public ParticipacionTemporada inscribir(Usuario usuario, Temporada temporada) {
        return participacionRepository.findByUsuarioIdAndTemporadaId(usuario.getId(), temporada.getId())
                .orElseGet(() -> {
                    int puntos = puntosIniciales(usuario);
                    Rango rango = catalogoService.rangoPara(puntos);
                    ParticipacionTemporada nueva = participacionRepository.save(ParticipacionTemporada.builder()
                            .id(new ParticipacionTemporada.Id(usuario.getId(), temporada.getId()))
                            .usuario(usuario)
                            .temporada(temporada)
                            .rangoInicial(rango)
                            .rangoActual(rango)
                            .puntosIniciales(puntos)
                            .puntosActuales(puntos)
                            .build());
                    historial(nueva, null, rango, puntos, CausaCambioRango.INSCRIPCION);
                    return nueva;
                });
    }

    /**
     * Suma (o resta) puntos sin bajar de 0 y, si cambió el rango, lo guarda en el historial y avisa.
     * Una participación que ya no está ACTIVA no suma nada.
     */
    @Transactional
    public void sumarPuntos(ParticipacionTemporada participacion, int delta) {
        if (participacion.getEstado() != EstadoParticipacionTemporada.ACTIVA) {
            return;
        }
        int puntos = Math.max(0, participacion.getPuntosActuales() + delta);
        Rango anterior = participacion.getRangoActual();
        Rango nuevo = catalogoService.rangoPara(puntos);
        participacion.setPuntosActuales(puntos);
        participacion.setRangoActual(nuevo);
        participacionRepository.save(participacion);
        if (!nuevo.getId().equals(anterior.getId())) {
            historial(participacion, anterior, nuevo, puntos, CausaCambioRango.ENFRENTAMIENTO);
            notificacionService.notificar(participacion.getUsuario(), TipoNotificacion.CAMBIO_RANGO,
                    "Cambiaste de rango", "Ahora estás en " + nuevo.nombre() + " con " + puntos + " puntos.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<ParticipacionTemporada> participacion(Long idUsuario, Long idTemporada) {
        return participacionRepository.findByUsuarioIdAndTemporadaId(idUsuario, idTemporada);
    }

    private void historial(ParticipacionTemporada p, Rango anterior, Rango nuevo, int puntos, CausaCambioRango causa) {
        historialRepository.save(HistorialRango.builder()
                .participacion(p).rangoAnterior(anterior).rangoNuevo(nuevo).puntos(puntos).causa(causa)
                .fechaCambio(LocalDateTime.now())
                .build());
    }

    /** Participaciones que cuentan en el ranking (ACTIVA o FINALIZADA), de más a menos puntos. */
    private List<ParticipacionTemporada> enRanking(Temporada temporada) {
        return participacionRepository.findByTemporadaIdAndEstadoInOrderByPuntosActualesDescFechaInscripcionAsc(
                temporada.getId(), EN_RANKING);
    }

    private int puntosIniciales(Usuario usuario) {
        Optional<ParticipacionTemporada> previa = participacionRepository
                .findFirstByUsuarioIdAndTemporadaEstadoOrderByTemporadaFechaFinDesc(usuario.getId(), EstadoTemporada.CERRADA)
                .filter(p -> p.getPuntosFinales() != null);
        if (previa.isEmpty()) {
            return 0;
        }
        ParticipacionTemporada anterior = previa.get();
        int puntos = anterior.getPuntosFinales() * anterior.getTemporada().getPorcentajeConservado() / 100;

        // Ascenso y descenso: solo para quien terminó la temporada (no para quien la abandonó) y solo en ligas
        // con más jugadores que cupos, para que nadie suba y baje a la vez.
        if (anterior.getEstado() != EstadoParticipacionTemporada.FINALIZADA) {
            return puntos;
        }
        // total y posición en la liga se cuentan en la base: no se carga la liga entera
        Liga liga = anterior.getRangoActual().getLiga();
        Long idTemporada = anterior.getTemporada().getId();
        long total = participacionRepository.contarFinalizadosEnLiga(idTemporada, liga.getId(),
                EstadoParticipacionTemporada.FINALIZADA);
        if (total > liga.getCuposAscenso() + liga.getCuposDescenso()) {
            long posicion = participacionRepository.adelanteEnLiga(idTemporada, liga.getId(), anterior.getPuntosFinales(),
                    anterior.getFechaInscripcion(), EstadoParticipacionTemporada.FINALIZADA);
            if (posicion < liga.getCuposAscenso()) {
                Optional<Rango> base = ligaRepository.findByOrden(liga.getOrden() + 1)
                        .flatMap(l -> rangoRepository.findFirstByLigaIdOrderByOrdenAsc(l.getId()));
                if (base.isPresent()) {
                    puntos = Math.max(puntos, base.get().getPuntosMinimo());
                }
            } else if (posicion >= total - liga.getCuposDescenso()) {
                Optional<Rango> tope = ligaRepository.findByOrden(liga.getOrden() - 1)
                        .flatMap(l -> rangoRepository.findFirstByLigaIdOrderByOrdenDesc(l.getId()));
                if (tope.isPresent()) {
                    puntos = Math.min(puntos, tope.get().getPuntosMinimo());
                }
            }
        }
        return puntos;
    }

    // ---------- administración ----------

    @Transactional(readOnly = true)
    public PaginaDTO<TemporadaDTO> listar(Pageable pagina) {
        return PaginaDTO.de(temporadaRepository.findAllByOrderByFechaInicioDescIdDesc(pagina).map(TemporadaDTO::de));
    }

    @Transactional
    public TemporadaDTO crear(GuardarTemporadaDTO dto) {
        validarFechas(dto);
        if (temporadaRepository.existsByNombre(dto.nombre().trim())) {
            throw new ConflictoException("Ya existe una temporada con ese nombre.");
        }
        return TemporadaDTO.de(temporadaRepository.save(aplicar(new Temporada(), dto)));
    }

    /** Solo se edita una temporada PROGRAMADA. */
    @Transactional
    public TemporadaDTO editar(Long idTemporada, GuardarTemporadaDTO dto) {
        Temporada temporada = buscar(idTemporada);
        if (temporada.getEstado() != EstadoTemporada.PROGRAMADA) {
            throw new ReglaNegocioException("Solo se puede editar una temporada programada.");
        }
        validarFechas(dto);
        if (temporadaRepository.existsByNombreAndIdNot(dto.nombre().trim(), idTemporada)) {
            throw new ConflictoException("Ya existe una temporada con ese nombre.");
        }
        return TemporadaDTO.de(temporadaRepository.save(aplicar(temporada, dto)));
    }

    @Transactional
    public TemporadaDTO activar(Long idTemporada) {
        Temporada temporada = buscar(idTemporada);
        if (temporada.getEstado() != EstadoTemporada.PROGRAMADA) {
            throw new ReglaNegocioException("Solo se puede activar una temporada programada.");
        }
        temporadaActiva().ifPresent(t -> {
            throw new ConflictoException("Ya está activa la temporada \"" + t.getNombre() + "\". Ciérrala primero.");
        });
        temporada.setEstado(EstadoTemporada.ACTIVA);
        return TemporadaDTO.de(temporadaRepository.save(temporada));
    }

    /**
     * Foto final. ACTIVA pasa a FINALIZADA con puntos y posición; ABANDONADA guarda sus puntos (cuentan para
     * la próxima temporada) sin posición; DESCALIFICADA queda sin puntos finales. La temporada queda CERRADA.
     */
    @Transactional
    public TemporadaDTO cerrar(Temporada temporada) {
        participacionRepository.fijarPuntosFinales(temporada.getId(), EstadoParticipacionTemporada.ABANDONADA);
        List<ParticipacionTemporada> ranking = enRanking(temporada);
        for (int i = 0; i < ranking.size(); i++) {
            ParticipacionTemporada p = ranking.get(i);
            p.setPuntosFinales(p.getPuntosActuales());
            p.setPosicionFinal(i + 1);
            p.setEstado(EstadoParticipacionTemporada.FINALIZADA);
            participacionRepository.save(p);
            notificacionService.notificar(p.getUsuario(), TipoNotificacion.FIN_TEMPORADA,
                    temporada.getNombre() + " cerrada",
                    "Terminaste en la posición " + (i + 1) + " con " + p.getPuntosActuales() + " puntos ("
                            + p.getRangoActual().nombre() + ").");
        }
        temporada.setEstado(EstadoTemporada.CERRADA);
        return TemporadaDTO.de(temporadaRepository.save(temporada));
    }

    @Transactional(readOnly = true)
    public Temporada buscar(Long idTemporada) {
        return temporadaRepository.findById(idTemporada)
                .orElseThrow(() -> new NoEncontradoException("La temporada no existe."));
    }

    /** posicion: la final si ya cerró; la actual si está ACTIVA; null si abandonó o fue descalificado. */
    private ParticipacionTemporadaDTO dto(ParticipacionTemporada p) {
        Integer posicion = p.getPosicionFinal() != null ? p.getPosicionFinal()
                : p.getEstado() != EstadoParticipacionTemporada.ACTIVA ? null
                : (int) participacionRepository.countByTemporadaIdAndEstadoAndPuntosActualesGreaterThan(
                        p.getTemporada().getId(), EstadoParticipacionTemporada.ACTIVA, p.getPuntosActuales()) + 1;
        return new ParticipacionTemporadaDTO(TemporadaDTO.de(p.getTemporada()), p.getPuntosIniciales(),
                p.getPuntosActuales(), RangoDTO.de(p.getRangoInicial()), RangoDTO.de(p.getRangoActual()), posicion,
                p.getPuntosFinales(), p.getEstado().name(), p.getFechaInscripcion());
    }

    private static void validarFechas(GuardarTemporadaDTO dto) {
        if (!dto.fechaFin().isAfter(dto.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la de inicio.");
        }
        if (dto.fechaFin().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La temporada no puede terminar en el pasado.");
        }
    }

    private static Temporada aplicar(Temporada temporada, GuardarTemporadaDTO dto) {
        temporada.setNombre(dto.nombre().trim());
        temporada.setFechaInicio(dto.fechaInicio());
        temporada.setFechaFin(dto.fechaFin());
        temporada.setPorcentajeConservado(dto.porcentajeConservado());
        if (temporada.getEstado() == null) {
            temporada.setEstado(EstadoTemporada.PROGRAMADA);
        }
        return temporada;
    }
}
