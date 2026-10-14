package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.reto.CrearRetoDTO;
import co.edu.uniquindio.kinet.dto.reto.ParticipacionDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.mappers.RetoMapper;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.NivelInicial;
import co.edu.uniquindio.kinet.model.enums.TipoMeta;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.ParticipacionRetoRepository;
import co.edu.uniquindio.kinet.repositories.RetoRepository;
import co.edu.uniquindio.kinet.repositories.TipoActividadRepository;
import co.edu.uniquindio.kinet.services.EvaluacionService;
import co.edu.uniquindio.kinet.services.NotificacionService;
import co.edu.uniquindio.kinet.services.RetoService;
import co.edu.uniquindio.kinet.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RetoServiceImpl implements RetoService {

    private final RetoRepository retoRepository;
    private final ParticipacionRetoRepository participacionRepository;
    private final TipoActividadRepository actividadRepository;
    private final UsuarioService usuarioService;
    private final EvaluacionService evaluacionService;
    private final NotificacionService notificacionService;
    private final RetoMapper retoMapper;

    @Override
    @Transactional
    public List<RetoDTO> listarDisponibles(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        NivelInicial nivel = nivelObligatorio(usuario);
        vencerParticipaciones(usuario);
        Map<Long, ParticipacionReto> ultimas = ultimasParticipaciones(usuario);

        return retoRepository.findByActivoOrderByXpRecompensaAsc(true).stream()
                .filter(reto -> reto.aplicaParaNivel(nivel))
                .map(reto -> retoMapper.toRetoDTO(reto, participacionDTO(ultimas.get(reto.getId()))))
                .toList();
    }

    @Override
    @Transactional
    public RetoDTO detalle(String correo, Long idReto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        vencerParticipaciones(usuario);
        Reto reto = buscarReto(idReto);
        Optional<ParticipacionReto> participacion =
                participacionRepository.findFirstByUsuarioIdAndRetoIdOrderByFechaInicioDesc(usuario.getId(), idReto);
        // un reto inactivo solo lo puede ver quien participó en él
        if (!Boolean.TRUE.equals(reto.getActivo()) && participacion.isEmpty()) {
            throw new NoEncontradoException("El reto no existe.");
        }
        return retoMapper.toRetoDTO(reto, participacionDTO(participacion.orElse(null)));
    }

    @Override
    @Transactional
    public ParticipacionDTO unirse(String correo, Long idReto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        NivelInicial nivel = nivelObligatorio(usuario);
        Reto reto = buscarReto(idReto);
        if (!Boolean.TRUE.equals(reto.getActivo())) {
            throw new ReglaNegocioException("Este reto ya no está disponible.");
        }
        if (!reto.aplicaParaNivel(nivel)) {
            throw new ReglaNegocioException("Este reto no corresponde a tu nivel.");
        }
        vencerParticipaciones(usuario);
        if (participacionRepository.existsByUsuarioIdAndRetoIdAndEstado(usuario.getId(), idReto,
                EstadoParticipacion.EN_CURSO)) {
            throw new ConflictoException("Ya estás participando en este reto.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        ParticipacionReto participacion = participacionRepository.save(ParticipacionReto.builder()
                .reto(reto)
                .usuario(usuario)
                .fechaInicio(ahora)
                .fechaLimite(ahora.plusDays(reto.getDuracionDias()))
                .build());
        return retoMapper.toParticipacionDTO(participacion);
    }

    @Override
    @Transactional
    public List<ParticipacionDTO> misRetos(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        vencerParticipaciones(usuario);
        return participacionRepository.findByUsuarioIdOrderByFechaInicioDesc(usuario.getId()).stream()
                .map(retoMapper::toParticipacionDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<ParticipacionReto> avanzarRetos(Usuario usuario, Entrenamiento entrenamiento) {
        vencerParticipaciones(usuario);
        List<ParticipacionReto> completados = new ArrayList<>();
        for (ParticipacionReto participacion :
                participacionRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoParticipacion.EN_CURSO)) {
            if (!cuentaParaReto(participacion, entrenamiento)) {
                continue;
            }
            Reto reto = participacion.getReto();
            int avance = reto.getTipoMeta() == TipoMeta.MINUTOS ? entrenamiento.getDuracionMinutos() : 1;
            participacion.setProgreso(participacion.getProgreso() + avance);

            if (participacion.getProgreso() >= reto.getMetaValor()) {
                participacion.setEstado(EstadoParticipacion.COMPLETADO);
                participacion.setFechaCompletado(LocalDateTime.now());
                completados.add(participacion);
                notificacionService.notificar(usuario, TipoNotificacion.RETO_COMPLETADO,
                        "¡Completaste el reto \"" + reto.getTitulo() + "\"! +" + reto.getXpRecompensa() + " XP.");
            }
            participacionRepository.save(participacion);
        }
        return completados;
    }

    @Override
    @Transactional
    public RetoDTO crear(CrearRetoDTO crearDTO) {
        TipoActividad actividad = null;
        if (crearDTO.codigoActividad() != null && !crearDTO.codigoActividad().isBlank()) {
            actividad = actividadRepository.findByCodigo(crearDTO.codigoActividad().trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new NoEncontradoException("La actividad no existe."));
        }
        Reto reto = retoRepository.save(Reto.builder()
                .titulo(crearDTO.titulo().trim())
                .descripcion(crearDTO.descripcion().trim())
                .tipoMeta(crearDTO.tipoMeta())
                .metaValor(crearDTO.metaValor())
                .tipoActividad(actividad)
                .nivelRequerido(crearDTO.nivelRequerido())
                .duracionDias(crearDTO.duracionDias())
                .xpRecompensa(crearDTO.xpRecompensa())
                .build());
        return retoMapper.toRetoDTO(reto, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetoDTO> listarTodos() {
        return retoRepository.findAll(Sort.by("id")).stream()
                .map(reto -> retoMapper.toRetoDTO(reto, null))
                .toList();
    }

    @Override
    @Transactional
    public RetoDTO cambiarEstado(Long idReto, boolean activo) {
        Reto reto = buscarReto(idReto);
        reto.setActivo(activo);
        return retoMapper.toRetoDTO(retoRepository.save(reto), null);
    }

    // ---------- apoyo ----------

    private Reto buscarReto(Long idReto) {
        return retoRepository.findById(idReto)
                .orElseThrow(() -> new NoEncontradoException("El reto no existe."));
    }

    /** Los retos se asignan según el nivel inicial: sin evaluación no hay retos. */
    private NivelInicial nivelObligatorio(Usuario usuario) {
        return evaluacionService.nivelDe(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException("Primero haz tu evaluación de nivel para ver tus retos."));
    }

    /** Las participaciones en curso cuyo plazo ya pasó se marcan como vencidas. */
    private void vencerParticipaciones(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        for (ParticipacionReto p :
                participacionRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoParticipacion.EN_CURSO)) {
            if (p.getFechaLimite().isBefore(ahora)) {
                p.setEstado(EstadoParticipacion.VENCIDO);
                participacionRepository.save(p);
            }
        }
    }

    /**
     * Un entrenamiento cuenta si se registró mientras el reto estaba en curso y es de la actividad pedida (si la hay).
     * Se usa la fecha de registro y no la de inicio porque el jugador reporta la sesión después de hacerla;
     * el anti-trampa ya impide reportar sesiones de hace más de 48 horas.
     */
    private static boolean cuentaParaReto(ParticipacionReto participacion, Entrenamiento entrenamiento) {
        LocalDateTime registro = entrenamiento.getFechaRegistro();
        if (registro.isBefore(participacion.getFechaInicio()) || registro.isAfter(participacion.getFechaLimite())) {
            return false;
        }
        TipoActividad requerida = participacion.getReto().getTipoActividad();
        return requerida == null || requerida.getId().equals(entrenamiento.getTipoActividad().getId());
    }

    /** Última participación del usuario en cada reto. */
    private Map<Long, ParticipacionReto> ultimasParticipaciones(Usuario usuario) {
        Map<Long, ParticipacionReto> ultimas = new HashMap<>();
        for (ParticipacionReto p : participacionRepository.findByUsuarioIdOrderByFechaInicioDesc(usuario.getId())) {
            ultimas.putIfAbsent(p.getReto().getId(), p);
        }
        return ultimas;
    }

    private ParticipacionDTO participacionDTO(ParticipacionReto participacion) {
        return participacion == null ? null : retoMapper.toParticipacionDTO(participacion);
    }
}
