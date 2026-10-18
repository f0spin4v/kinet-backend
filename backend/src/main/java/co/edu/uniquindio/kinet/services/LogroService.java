package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.logro.GuardarLogroDTO;
import co.edu.uniquindio.kinet.dto.logro.LogroDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.model.Logro;
import co.edu.uniquindio.kinet.model.LogroUsuario;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.ResultadoEnfrentamiento;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Logros: catálogo, evaluación automática de criterios y su recompensa de XP. */
@Service
@RequiredArgsConstructor
public class LogroService {

    private final LogroRepository logroRepository;
    private final LogroUsuarioRepository logroUsuarioRepository;
    private final PerfilJugadorRepository perfilRepository;
    private final UsuarioService usuarioService;
    private final CatalogoService catalogoService;
    private final ProgresoService progresoService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<LogroDTO> catalogo(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return logroRepository.catalogoDe(usuario.getId()).stream()
                .map(f -> LogroDTO.de(f.getLogro(), f.getFechaObtenido()))
                .toList();
    }

    /**
     * Revisa los logros que el jugador aún no tiene y otorga los que ya cumple. Se repite mientras aparezcan
     * logros nuevos, porque el XP de un logro puede hacer cumplir otro (por ejemplo uno de NIVEL).
     * Cada vuelta hace una sola consulta de métricas.
     *
     * @return nombres de los logros desbloqueados.
     */
    @Transactional
    public List<String> evaluar(Usuario usuario) {
        List<String> nuevos = new ArrayList<>();
        List<Logro> pendientes = new ArrayList<>(logroRepository.pendientesDe(usuario.getId()));
        boolean hubo = true;
        while (hubo && !pendientes.isEmpty()) {
            hubo = false;
            Map<CriterioLogro.Metrica, Long> metricas = metricas(usuario.getId());
            for (Iterator<Logro> it = pendientes.iterator(); it.hasNext(); ) {
                Logro logro = it.next();
                CriterioLogro criterio = CriterioLogro.leer(logro.getCriterio());
                if (!criterio.seCumple(metricas.get(criterio.metrica()))) {
                    continue;
                }
                logroUsuarioRepository.save(LogroUsuario.builder()
                        .id(new LogroUsuario.Id(usuario.getId(), logro.getId()))
                        .usuario(usuario)
                        .logro(logro)
                        .build());
                if (logro.getXpBonus() > 0) {
                    progresoService.porLogro(usuario, logro, logro.getXpBonus());
                }
                notificacionService.notificar(usuario, TipoNotificacion.LOGRO, "Logro desbloqueado",
                        logro.getNombre() + (logro.getXpBonus() > 0 ? ". +" + logro.getXpBonus() + " XP." : "."));
                it.remove();
                nuevos.add(logro.getNombre());
                hubo = true;
            }
        }
        return nuevos;
    }

    private Map<CriterioLogro.Metrica, Long> metricas(Long idUsuario) {
        PerfilJugadorRepository.MetricasLogro r = perfilRepository.metricasLogro(idUsuario, EstadoValidacion.VALIDADO,
                EstadoParticipacion.COMPLETADO, ResultadoEnfrentamiento.VICTORIA);
        Map<CriterioLogro.Metrica, Long> m = new EnumMap<>(CriterioLogro.Metrica.class);
        m.put(CriterioLogro.Metrica.ENTRENAMIENTOS, r.getEntrenamientos());
        m.put(CriterioLogro.Metrica.MINUTOS, r.getMinutos());
        m.put(CriterioLogro.Metrica.RACHA, (long) r.getRacha());
        m.put(CriterioLogro.Metrica.XP, (long) r.getXp());
        m.put(CriterioLogro.Metrica.NIVEL, (long) catalogoService.nivelPara(r.getXp()));
        m.put(CriterioLogro.Metrica.RETOS, r.getRetos());
        m.put(CriterioLogro.Metrica.VICTORIAS, r.getVictorias());
        m.put(CriterioLogro.Metrica.CLAN, r.getClanes());
        return m;
    }

    // ---------- administración ----------

    @Transactional
    public LogroDTO crear(GuardarLogroDTO dto) {
        if (logroRepository.existsByNombre(dto.nombre().trim())) {
            throw new ConflictoException("Ya existe un logro con ese nombre.");
        }
        return LogroDTO.de(logroRepository.save(aplicar(new Logro(), dto)), null);
    }

    @Transactional
    public LogroDTO editar(Long idLogro, GuardarLogroDTO dto) {
        Logro logro = buscar(idLogro);
        if (logroRepository.existsByNombreAndIdNot(dto.nombre().trim(), idLogro)) {
            throw new ConflictoException("Ya existe un logro con ese nombre.");
        }
        return LogroDTO.de(logroRepository.save(aplicar(logro, dto)), null);
    }

    /** Solo se puede borrar un logro que nadie ha obtenido; si ya se otorgó, se conserva el historial. */
    @Transactional
    public void eliminar(Long idLogro) {
        Logro logro = buscar(idLogro);
        if (logroUsuarioRepository.existsByLogroId(idLogro)) {
            throw new ConflictoException("Este logro ya lo obtuvieron jugadores y no se puede borrar.");
        }
        logroRepository.delete(logro);
    }

    private static Logro aplicar(Logro logro, GuardarLogroDTO dto) {
        CriterioLogro.leer(dto.criterio());
        logro.setNombre(dto.nombre().trim());
        logro.setDescripcion(dto.descripcion().trim());
        logro.setCriterio(dto.criterio().trim());
        logro.setXpBonus(dto.xpBonus());
        logro.setIconoUrl(dto.iconoUrl() == null || dto.iconoUrl().isBlank() ? null : dto.iconoUrl().trim());
        return logro;
    }

    private Logro buscar(Long idLogro) {
        return logroRepository.findById(idLogro).orElseThrow(() -> new NoEncontradoException("El logro no existe."));
    }
}
