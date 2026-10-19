package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.reto.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.*;
import co.edu.uniquindio.kinet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Retos y misiones. Cada participación es de un periodo (el día, la semana, el reto único o la temporada), así
 * los retos DIARIOS y SEMANALES se repiten. El avance son los APORTE_RETO: automáticos al validar entrenamientos
 * (MINUTOS, SESIONES) o manuales en los retos de REPETICIONES. En los retos de CLAN la meta se compara con la
 * suma del clan.
 */
@Service
@RequiredArgsConstructor
public class RetoService {

    private final RetoRepository retoRepository;
    private final ParticipacionRetoRepository participacionRepository;
    private final AporteRetoRepository aporteRepository;
    private final MembresiaClanRepository membresiaRepository;
    private final UsuarioService usuarioService;
    private final EvaluacionService evaluacionService;
    private final CatalogoService catalogoService;
    private final TemporadaService temporadaService;
    private final ProgresoService progresoService;
    private final LogroService logroService;
    private final NotificacionService notificacionService;

    /** Inicio del periodo (día) y momento en que vence la participación. */
    public record Periodo(LocalDate inicio, LocalDateTime limite) {
    }

    // ---------- jugador ----------

    /** Retos activos que el jugador puede ver: los de todos los niveles y los de su nivel vigente. */
    @Transactional
    public PaginaDTO<RetoDTO> disponibles(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        vencerDe(usuario);
        Long idNivel = evaluacionService.nivelVigente(usuario.getId()).map(NivelHabilidad::getId).orElse(-1L);
        LocalDateTime ahora = LocalDateTime.now();
        Page<Reto> pagRetos = retoRepository.vigentesPara(ahora, idNivel, EstadoReto.ACTIVO, Periodicidad.TEMPORADA,
                EstadoTemporada.ACTIVA, pagina);
        return PaginaDTO.de(pagRetos, retos -> {
            // todas las participaciones del jugador en los retos de la página en una consulta; luego se elige la
            // del periodo actual
            Map<Long, List<ParticipacionReto>> mias = retos.isEmpty() ? Map.of()
                    : participacionRepository.findByUsuarioIdAndRetoIdIn(usuario.getId(),
                            retos.stream().map(Reto::getId).toList()).stream()
                    .collect(Collectors.groupingBy(p -> p.getReto().getId()));
            return retos.stream()
                    .map(r -> {
                        LocalDate periodo = periodo(r, ahora).inicio();
                        ParticipacionDTO actual = mias.getOrDefault(r.getId(), List.of()).stream()
                                .filter(p -> p.getPeriodo().equals(periodo)).findFirst().map(this::dto).orElse(null);
                        return RetoDTO.de(r, actual);
                    })
                    .toList();
        });
    }

    @Transactional
    public RetoDTO detalle(String correo, Long idReto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        vencerDe(usuario);
        Reto reto = buscar(idReto);
        Optional<ParticipacionReto> participacion = participacionActual(reto, usuario, LocalDateTime.now());
        // un reto que no está activo solo lo ve quien participó en él
        if (reto.getEstado() != EstadoReto.ACTIVO && participacion.isEmpty()) {
            throw new NoEncontradoException("El reto no existe.");
        }
        return RetoDTO.de(reto, participacion.map(this::dto).orElse(null));
    }

    @Transactional
    public ParticipacionDTO unirse(String correo, Long idReto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Reto reto = buscar(idReto);
        LocalDateTime ahora = LocalDateTime.now();
        if (reto.getEstado() != EstadoReto.ACTIVO || !estaVigente(reto, ahora)) {
            throw new ReglaNegocioException("Este reto no está disponible.");
        }
        if (reto.getNivelHabilidad() != null) {
            NivelHabilidad nivel = evaluacionService.nivelVigente(usuario.getId())
                    .orElseThrow(() -> new ReglaNegocioException("Primero haz tu evaluación de nivel para unirte a este reto."));
            if (!nivel.getId().equals(reto.getNivelHabilidad().getId())) {
                throw new ReglaNegocioException("Este reto es para el nivel " + reto.getNivelHabilidad().getNombre() + ".");
            }
        }
        Clan clan = null;
        if (reto.getAmbito() == AmbitoReto.CLAN) {
            clan = membresiaRepository.findByUsuarioIdAndFechaSalidaIsNull(usuario.getId())
                    .map(MembresiaClan::getClan)
                    .orElseThrow(() -> new ReglaNegocioException("Este reto es de clan: primero únete a uno."));
        }
        Periodo periodo = periodo(reto, ahora);
        Optional<ParticipacionReto> previa = participacionRepository
                .findByRetoIdAndUsuarioIdAndPeriodo(idReto, usuario.getId(), periodo.inicio());
        if (previa.isPresent()) {
            throw new ConflictoException(mensajeDeParticipacionPrevia(reto, previa.get()));
        }
        ParticipacionReto participacion = participacionRepository.save(ParticipacionReto.builder()
                .reto(reto)
                .usuario(usuario)
                .clan(clan)
                .periodo(periodo.inicio())
                .fechaLimite(periodo.limite())
                .fechaInscripcion(ahora)
                .build());
        return dto(participacion);
    }

    @Transactional
    public ParticipacionDTO abandonar(String correo, Long idParticipacion) {
        ParticipacionReto participacion = propia(correo, idParticipacion);
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO) {
            throw new ReglaNegocioException("Solo puedes abandonar un reto en curso.");
        }
        participacion.setEstado(EstadoParticipacion.ABANDONADO);
        return dto(participacionRepository.save(participacion));
    }

    @Transactional
    public PaginaDTO<ParticipacionDTO> misRetos(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        vencerDe(usuario);
        return PaginaDTO.de(participacionRepository
                .findByUsuarioIdOrderByFechaInscripcionDescIdDesc(usuario.getId(), pagina).map(this::dto));
    }

    /** Aporte manual: solo en retos de REPETICIONES, que no se pueden medir con entrenamientos. */
    @Transactional
    public ParticipacionDTO aportarManual(String correo, Long idParticipacion, AporteManualDTO aporteDTO) {
        ParticipacionReto participacion = propia(correo, idParticipacion);
        if (participacion.getReto().getMetrica() != MetricaReto.REPETICIONES) {
            throw new ReglaNegocioException("Este reto avanza solo con tus entrenamientos validados.");
        }
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO
                || !participacion.getFechaLimite().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("Este reto ya no está en curso.");
        }
        aporteRepository.save(AporteReto.builder()
                .participacion(participacion)
                .cantidad(aporteDTO.cantidad())
                .nota(aporteDTO.nota() == null || aporteDTO.nota().isBlank() ? null : aporteDTO.nota().trim())
                .build());
        participacion.setProgreso(participacion.getProgreso().add(aporteDTO.cantidad()));
        participacionRepository.save(participacion);
        verificarCompletado(participacion);
        logroService.evaluar(participacion.getUsuario());
        return dto(participacion);
    }

    /**
     * Suma un entrenamiento validado a las participaciones en curso del jugador donde cuenta: empezó dentro del
     * periodo, terminó después de inscribirse en el reto y es de la actividad pedida (si la hay).
     * Distancia y calorías son informativas: no cuentan.
     *
     * @return las participaciones del jugador que quedaron completadas.
     */
    @Transactional
    public List<ParticipacionReto> registrarAportes(Entrenamiento entrenamiento) {
        List<ParticipacionReto> completadas = new ArrayList<>();
        LocalDateTime inicio = entrenamiento.getFechaHoraInicio();
        for (ParticipacionReto p : participacionRepository.dondeCuenta(entrenamiento.getUsuario().getId(),
                entrenamiento.getId(), entrenamiento.getTipoActividad().getId(), inicio, inicio.toLocalDate(),
                entrenamiento.getFechaHoraFin(), List.of(MetricaReto.MINUTOS, MetricaReto.SESIONES),
                EstadoParticipacion.EN_CURSO)) {
            BigDecimal cantidad = cantidad(p.getReto().getMetrica(), entrenamiento);
            aporteRepository.save(AporteReto.builder()
                    .participacion(p)
                    .entrenamiento(entrenamiento)
                    .fechaHora(entrenamiento.getFechaRegistro())
                    .cantidad(cantidad)
                    .build());
            p.setProgreso(p.getProgreso().add(cantidad));
            participacionRepository.save(p);
            if (verificarCompletado(p)) {
                completadas.add(p);
            }
        }
        return completadas;
    }

    /** Las participaciones en curso cuyo plazo ya pasó quedan VENCIDAS. Lo llama la tarea programada. */
    @Transactional
    public int vencerTodas() {
        return participacionRepository.vencerTodas(LocalDateTime.now(), EstadoParticipacion.EN_CURSO,
                EstadoParticipacion.VENCIDO);
    }

    // ---------- administración ----------

    @Transactional(readOnly = true)
    public PaginaDTO<RetoDTO> listarTodos(Pageable pagina) {
        return PaginaDTO.de(retoRepository.findAllByOrderByIdAsc(pagina).map(r -> RetoDTO.de(r, null)));
    }

    /** Los retos nacen en BORRADOR; se publican con cambiarEstado(ACTIVO). */
    @Transactional
    public RetoDTO crear(GuardarRetoDTO dto) {
        Reto reto = aplicar(Reto.builder().estado(EstadoReto.BORRADOR).build(), dto);
        return RetoDTO.de(retoRepository.save(reto), null);
    }

    @Transactional
    public RetoDTO editar(Long idReto, GuardarRetoDTO dto) {
        Reto reto = buscar(idReto);
        if (reto.getEstado() != EstadoReto.BORRADOR) {
            throw new ReglaNegocioException("Solo se puede editar un reto en borrador.");
        }
        return RetoDTO.de(retoRepository.save(aplicar(reto, dto)), null);
    }

    /** BORRADOR -> ACTIVO -> FINALIZADO. Al finalizar, las participaciones en curso vencen. */
    @Transactional
    public RetoDTO cambiarEstado(Long idReto, EstadoReto estado) {
        Reto reto = buscar(idReto);
        boolean valido = (reto.getEstado() == EstadoReto.BORRADOR && estado == EstadoReto.ACTIVO)
                || (reto.getEstado() == EstadoReto.ACTIVO && estado == EstadoReto.FINALIZADO);
        if (!valido) {
            throw new ReglaNegocioException("Un reto en " + reto.getEstado() + " no puede pasar a " + estado + ".");
        }
        reto.setEstado(estado);
        if (estado == EstadoReto.FINALIZADO) {
            participacionRepository.cambiarEstadoDeReto(idReto, EstadoParticipacion.EN_CURSO, EstadoParticipacion.VENCIDO);
        }
        return RetoDTO.de(retoRepository.save(reto), null);
    }

    // ---------- reglas ----------

    /** Una participación por periodo: si la anterior ya terminó (vencida, abandonada o completada) no se repite. */
    private static String mensajeDeParticipacionPrevia(Reto reto, ParticipacionReto previa) {
        String siguiente = reto.getPeriodicidad() == Periodicidad.DIARIA ? "Podrás volver a unirte mañana."
                : reto.getPeriodicidad() == Periodicidad.SEMANAL ? "Podrás volver a unirte la próxima semana."
                : "Este reto no se repite.";
        return switch (previa.getEstado()) {
            case EN_CURSO -> "Ya participas en este reto en el periodo actual.";
            case COMPLETADO -> "Ya completaste este reto en este periodo. " + siguiente;
            case VENCIDO -> "Dejaste vencer este reto: perdiste la oportunidad de este periodo. " + siguiente;
            case ABANDONADO -> "Abandonaste este reto y no puedes retomarlo en este periodo. " + siguiente;
        };
    }

    /** Periodo de un reto en un momento dado. */
    Periodo periodo(Reto reto, LocalDateTime momento) {
        LocalDate dia = momento.toLocalDate();
        Periodo periodo = switch (reto.getPeriodicidad()) {
            case DIARIA -> new Periodo(dia, dia.plusDays(1).atStartOfDay());
            case SEMANAL -> {
                LocalDate lunes = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Periodo(lunes, lunes.plusDays(7).atStartOfDay());
            }
            case UNICA -> new Periodo(reto.getFechaInicio().toLocalDate(), reto.getFechaFin() != null
                    ? reto.getFechaFin() : momento.plusDays(ReglasJuego.DIAS_RETO_UNICO_SIN_FIN));
            case TEMPORADA -> new Periodo(reto.getTemporada().getFechaInicio(),
                    reto.getTemporada().getFechaFin().plusDays(1).atStartOfDay());
        };
        if (reto.getFechaFin() != null && periodo.limite().isAfter(reto.getFechaFin())) {
            return new Periodo(periodo.inicio(), reto.getFechaFin());
        }
        return periodo;
    }

    /** La misma regla que aplica RetoRepository.vigentesPara, para validar un reto puntual. */
    private boolean estaVigente(Reto reto, LocalDateTime ahora) {
        if (reto.getFechaInicio().isAfter(ahora) || (reto.getFechaFin() != null && !reto.getFechaFin().isAfter(ahora))) {
            return false;
        }
        return reto.getPeriodicidad() != Periodicidad.TEMPORADA
                || reto.getTemporada().getEstado() == EstadoTemporada.ACTIVA;
    }

    private Optional<ParticipacionReto> participacionActual(Reto reto, Usuario usuario, LocalDateTime ahora) {
        if (reto.getPeriodicidad() == Periodicidad.TEMPORADA && reto.getTemporada() == null) {
            return Optional.empty();
        }
        return participacionRepository.findByRetoIdAndUsuarioIdAndPeriodo(reto.getId(), usuario.getId(),
                periodo(reto, ahora).inicio());
    }

    private static BigDecimal cantidad(MetricaReto metrica, Entrenamiento e) {
        return switch (metrica) {
            case MINUTOS -> BigDecimal.valueOf(e.getDuracionMin());
            case SESIONES -> BigDecimal.ONE;
            case REPETICIONES -> BigDecimal.ZERO;
        };
    }

    /**
     * Si la meta se alcanzó, completa la participación (o todas las del clan en ese periodo) y paga el XP.
     *
     * @return true si la participación dada quedó completada.
     */
    private boolean verificarCompletado(ParticipacionReto p) {
        Reto reto = p.getReto();
        if (reto.getAmbito() == AmbitoReto.INDIVIDUAL) {
            if (p.getProgreso().compareTo(reto.getValorMeta()) >= 0) {
                completar(p, "Completaste \"" + reto.getNombre() + "\". +" + reto.getXpRecompensa() + " XP.");
                return true;
            }
            return false;
        }
        BigDecimal delClan = participacionRepository.progresoDelClan(reto.getId(), p.getClan().getId(), p.getPeriodo());
        if (delClan.compareTo(reto.getValorMeta()) < 0) {
            return false;
        }
        for (ParticipacionReto q : participacionRepository.findByRetoIdAndClanIdAndPeriodoAndEstado(reto.getId(),
                p.getClan().getId(), p.getPeriodo(), EstadoParticipacion.EN_CURSO)) {
            completar(q, "Tu clan completó \"" + reto.getNombre() + "\". +" + reto.getXpRecompensa() + " XP.");
        }
        return true;
    }

    private void completar(ParticipacionReto p, String mensaje) {
        p.setEstado(EstadoParticipacion.COMPLETADO);
        p.setFechaCompletado(LocalDateTime.now());
        participacionRepository.save(p);
        progresoService.porReto(p.getUsuario(), p, p.getReto().getXpRecompensa());
        notificacionService.notificar(p.getUsuario(), TipoNotificacion.RETO, "Reto completado", mensaje);
    }

    private void vencerDe(Usuario usuario) {
        participacionRepository.vencerDe(usuario.getId(), LocalDateTime.now(), EstadoParticipacion.EN_CURSO,
                EstadoParticipacion.VENCIDO);
    }

    ParticipacionDTO dto(ParticipacionReto p) {
        Reto reto = p.getReto();
        BigDecimal delClan = reto.getAmbito() == AmbitoReto.CLAN && p.getClan() != null
                ? participacionRepository.progresoDelClan(reto.getId(), p.getClan().getId(), p.getPeriodo()) : null;
        BigDecimal avance = delClan != null ? delClan : p.getProgreso();
        int porcentaje = avance.multiply(BigDecimal.valueOf(100))
                .divide(reto.getValorMeta(), 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100)).intValue();
        return new ParticipacionDTO(p.getId(), reto.getId(), reto.getNombre(), reto.getMetrica().name(),
                reto.getAmbito().name(), p.getPeriodo(), p.getFechaLimite(), p.getProgreso(), delClan,
                reto.getValorMeta(), porcentaje, p.getEstado().name(), p.getFechaInscripcion(), p.getFechaCompletado());
    }

    private ParticipacionReto propia(String correo, Long idParticipacion) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return participacionRepository.findByIdAndUsuarioId(idParticipacion, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("La participación no existe."));
    }

    private Reto buscar(Long idReto) {
        return retoRepository.findById(idReto).orElseThrow(() -> new NoEncontradoException("El reto no existe."));
    }

    private Reto aplicar(Reto reto, GuardarRetoDTO dto) {
        if (dto.fechaFin() != null && !dto.fechaFin().isAfter(dto.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la de inicio.");
        }
        Temporada temporada = null;
        if (dto.periodicidad() == Periodicidad.TEMPORADA) {
            if (dto.idTemporada() == null) {
                throw new ReglaNegocioException("Un reto de temporada necesita la temporada.");
            }
            temporada = temporadaService.buscar(dto.idTemporada());
        } else if (dto.idTemporada() != null) {
            throw new ReglaNegocioException("Solo los retos de periodicidad TEMPORADA se asocian a una temporada.");
        }
        reto.setNombre(dto.nombre().trim());
        reto.setDescripcion(dto.descripcion().trim());
        reto.setCondiciones(dto.condiciones() == null || dto.condiciones().isBlank() ? null : dto.condiciones().trim());
        reto.setImagenUrl(dto.imagenUrl() == null || dto.imagenUrl().isBlank() ? null : dto.imagenUrl().trim());
        reto.setMetrica(dto.metrica());
        reto.setValorMeta(dto.valorMeta());
        reto.setXpRecompensa(dto.xpRecompensa());
        reto.setPeriodicidad(dto.periodicidad());
        reto.setAmbito(dto.ambito());
        reto.setTipoActividad(dto.idTipoActividad() == null ? null : catalogoService.actividad(dto.idTipoActividad()));
        reto.setNivelHabilidad(dto.idNivelHabilidad() == null ? null : catalogoService.nivelHabilidad(dto.idNivelHabilidad()));
        reto.setTemporada(temporada);
        reto.setFechaInicio(dto.fechaInicio());
        reto.setFechaFin(dto.fechaFin());
        return reto;
    }
}
