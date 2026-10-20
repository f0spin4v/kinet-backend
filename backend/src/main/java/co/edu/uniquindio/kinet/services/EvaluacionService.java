package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.evaluacion.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.repositories.EvaluacionUsuarioRepository;
import co.edu.uniquindio.kinet.repositories.OpcionEvaluacionRepository;
import co.edu.uniquindio.kinet.repositories.PreguntaEvaluacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Proceso 1: evaluación de nivel (Placement). Cada opción vota por un nivel con su peso; gana el nivel más
 * votado y, en empate, el de menor orden (criterio conservador). Se puede repetir cada 30 días.
 */
@Service
@RequiredArgsConstructor
public class EvaluacionService {

    private final PreguntaEvaluacionRepository preguntaRepository;
    private final OpcionEvaluacionRepository opcionRepository;
    private final EvaluacionUsuarioRepository evaluacionRepository;
    private final UsuarioService usuarioService;
    private final CatalogoService catalogoService;

    @Transactional(readOnly = true)
    public List<PreguntaDTO> preguntas() {
        return preguntaRepository.findByActivaOrderByOrdenAsc(true).stream().map(PreguntaDTO::de).toList();
    }

    @Transactional
    public ResultadoEvaluacionDTO responder(String correo, RespuestasEvaluacionDTO respuestasDTO) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        evaluacionRepository.findFirstByUsuarioIdOrderByFechaEvaluacionDesc(usuario.getId())
                .map(e -> e.getFechaEvaluacion().plusDays(ReglasJuego.DIAS_ENTRE_EVALUACIONES))
                .filter(desde -> desde.isAfter(LocalDateTime.now()))
                .ifPresent(desde -> {
                    throw new ReglaNegocioException("Podrás repetir la evaluación desde el " + desde.toLocalDate() + ".");
                });

        List<PreguntaEvaluacion> activas = preguntaRepository.findByActivaOrderByOrdenAsc(true);
        Set<Long> ids = new LinkedHashSet<>(respuestasDTO.idsOpciones());
        List<OpcionEvaluacion> elegidas = opcionRepository.findAllById(ids);
        if (elegidas.size() != ids.size()) {
            throw new NoEncontradoException("Una de las opciones elegidas no existe.");
        }
        // exactamente una opción por cada pregunta activa
        Map<Long, Long> porPregunta = elegidas.stream()
                .collect(Collectors.groupingBy(o -> o.getPregunta().getId(), Collectors.counting()));
        Set<Long> idsActivas = activas.stream().map(PreguntaEvaluacion::getId).collect(Collectors.toSet());
        if (!porPregunta.keySet().equals(idsActivas) || porPregunta.values().stream().anyMatch(n -> n != 1)) {
            throw new ReglaNegocioException("Responde cada una de las " + activas.size() + " preguntas con una sola opción.");
        }

        EvaluacionUsuario evaluacion = evaluacionRepository.save(EvaluacionUsuario.builder()
                .usuario(usuario)
                .nivelHabilidad(ganador(elegidas))
                .versionAlgoritmo(ReglasJuego.VERSION_EVALUACION)
                .respuestas(new ArrayList<>(elegidas))
                .build());
        return resultado(evaluacion);
    }

    @Transactional(readOnly = true)
    public ResultadoEvaluacionDTO vigente(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return evaluacionRepository.findFirstByUsuarioIdOrderByFechaEvaluacionDesc(usuario.getId())
                .map(this::resultado)
                .orElseThrow(() -> new NoEncontradoException("Aún no has hecho tu evaluación de nivel."));
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ResultadoEvaluacionDTO> historial(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return PaginaDTO.de(evaluacionRepository
                .findByUsuarioIdOrderByFechaEvaluacionDescIdDesc(usuario.getId(), pagina).map(this::resultado));
    }

    /** Nivel vigente del jugador (el de su última evaluación), si ya se evaluó. */
    @Transactional(readOnly = true)
    public Optional<NivelHabilidad> nivelVigente(Long idUsuario) {
        return evaluacionRepository.findFirstByUsuarioIdOrderByFechaEvaluacionDesc(idUsuario)
                .map(EvaluacionUsuario::getNivelHabilidad);
    }

    // ---------- administración del cuestionario ----------

    @Transactional(readOnly = true)
    public List<PreguntaAdminDTO> preguntasAdmin() {
        return preguntaRepository.findAllByOrderByOrdenAsc().stream().map(PreguntaAdminDTO::de).toList();
    }

    @Transactional
    public PreguntaAdminDTO crearPregunta(GuardarPreguntaDTO dto) {
        if (preguntaRepository.existsByOrden(dto.orden())) {
            throw new ConflictoException("Ya hay una pregunta en la posición " + dto.orden() + ".");
        }
        PreguntaEvaluacion pregunta = PreguntaEvaluacion.builder()
                .enunciado(dto.enunciado().trim())
                .orden(dto.orden())
                .build();
        pregunta.getOpciones().addAll(opciones(pregunta, dto.opciones()));
        return PreguntaAdminDTO.de(preguntaRepository.save(pregunta));
    }

    /** Si la pregunta ya tiene respuestas solo se cambian enunciado y orden: las opciones quedan fijas. */
    @Transactional
    public PreguntaAdminDTO editarPregunta(Long idPregunta, GuardarPreguntaDTO dto) {
        PreguntaEvaluacion pregunta = buscarPregunta(idPregunta);
        if (preguntaRepository.existsByOrdenAndIdNot(dto.orden(), idPregunta)) {
            throw new ConflictoException("Ya hay una pregunta en la posición " + dto.orden() + ".");
        }
        pregunta.setEnunciado(dto.enunciado().trim());
        pregunta.setOrden(dto.orden());
        if (evaluacionRepository.existeRespuestaDePregunta(idPregunta)) {
            if (!mismasOpciones(pregunta, dto)) {
                throw new ReglaNegocioException("Esta pregunta ya tiene respuestas: sus opciones no se pueden cambiar. "
                        + "Desactívala y crea una nueva.");
            }
        } else {
            pregunta.getOpciones().clear();
            preguntaRepository.saveAndFlush(pregunta);
            pregunta.getOpciones().addAll(opciones(pregunta, dto.opciones()));
        }
        return PreguntaAdminDTO.de(preguntaRepository.save(pregunta));
    }

    @Transactional
    public PreguntaAdminDTO cambiarEstadoPregunta(Long idPregunta, boolean activa) {
        PreguntaEvaluacion pregunta = buscarPregunta(idPregunta);
        // se cuenta antes de cambiarla: si no, Hibernate guarda el cambio antes de la consulta y la deja fuera
        if (!activa && Boolean.TRUE.equals(pregunta.getActiva()) &&preguntaRepository.countByActiva(true) <= 1) {
            throw new ReglaNegocioException("El cuestionario debe tener al menos una pregunta activa.");
        }
        pregunta.setActiva(activa);
        return PreguntaAdminDTO.de(preguntaRepository.save(pregunta));
    }

    // ---------- apoyo ----------

    /** Nivel más votado; en empate, el de menor orden. */
    private static NivelHabilidad ganador(List<OpcionEvaluacion> elegidas) {
        Map<NivelHabilidad, Integer> votos = votos(elegidas);
        return votos.entrySet().stream()
                .max(Comparator.<Map.Entry<NivelHabilidad, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(e -> -e.getKey().getOrden()))
                .orElseThrow()
                .getKey();
    }

    private static Map<NivelHabilidad, Integer> votos(List<OpcionEvaluacion> opciones) {
        Map<Long, NivelHabilidad> niveles = opciones.stream().map(OpcionEvaluacion::getNivelHabilidad)
                .collect(Collectors.toMap(NivelHabilidad::getId, Function.identity(), (a, b) -> a));
        Map<NivelHabilidad, Integer> votos = new TreeMap<>(Comparator.comparing(NivelHabilidad::getOrden));
        opciones.forEach(o -> votos.merge(niveles.get(o.getNivelHabilidad().getId()), o.getPuntaje(), Integer::sum));
        return votos;
    }

    private ResultadoEvaluacionDTO resultado(EvaluacionUsuario evaluacion) {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos(evaluacion.getRespuestas()).forEach((nivel, peso) -> votos.put(nivel.getNombre(), peso));
        return new ResultadoEvaluacionDTO(evaluacion.getId(), evaluacion.getNivelHabilidad().getNombre(), votos,
                evaluacion.getVersionAlgoritmo(), evaluacion.getFechaEvaluacion(),
                evaluacion.getFechaEvaluacion().plusDays(ReglasJuego.DIAS_ENTRE_EVALUACIONES));
    }

    private List<OpcionEvaluacion> opciones(PreguntaEvaluacion pregunta, List<GuardarPreguntaDTO.OpcionNuevaDTO> nuevas) {
        List<OpcionEvaluacion> opciones = new ArrayList<>();
        Set<Long> niveles = new HashSet<>();
        for (GuardarPreguntaDTO.OpcionNuevaDTO nueva : nuevas) {
            NivelHabilidad nivel = catalogoService.nivelHabilidad(nueva.nivelHabilidad());
            if (!niveles.add(nivel.getId())) {
                throw new ReglaNegocioException("Cada opción de una pregunta debe votar por un nivel distinto.");
            }
            opciones.add(OpcionEvaluacion.builder()
                    .pregunta(pregunta)
                    .nivelHabilidad(nivel)
                    .titulo(nueva.titulo().trim())
                    .descripcion(nueva.descripcion() == null ? null : nueva.descripcion().trim())
                    .puntaje(nueva.puntaje())
                    .build());
        }
        return opciones;
    }

    private static boolean mismasOpciones(PreguntaEvaluacion pregunta, GuardarPreguntaDTO dto) {
        List<String> actuales = pregunta.getOpciones().stream()
                .map(o -> o.getTitulo() + "|" + o.getNivelHabilidad().getNombre() + "|" + o.getPuntaje()).sorted().toList();
        List<String> nuevas = dto.opciones().stream()
                .map(o -> o.titulo().trim() + "|" + o.nivelHabilidad().trim().toUpperCase(Locale.ROOT) + "|" + o.puntaje())
                .sorted().toList();
        return actuales.equals(nuevas);
    }

    private PreguntaEvaluacion buscarPregunta(Long idPregunta) {
        return preguntaRepository.findById(idPregunta)
                .orElseThrow(() -> new NoEncontradoException("La pregunta no existe."));
    }
}
