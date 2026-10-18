package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.analitica.ResumenAnaliticaDTO;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Indicadores agregados para el administrador. Solo conteos: no expone datos personales. */
@Service
@RequiredArgsConstructor
public class AnaliticaService {

    private final UsuarioRepository usuarioRepository;
    private final EvaluacionUsuarioRepository evaluacionRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final ParticipacionRetoRepository participacionRetoRepository;
    private final ParticipacionTemporadaRepository participacionTemporadaRepository;
    private final NotificacionRepository notificacionRepository;
    private final CompetenciaRepository competenciaRepository;

    @Transactional(readOnly = true)
    public ResumenAnaliticaDTO resumen() {
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        return new ResumenAnaliticaDTO(
                mapa(usuarioRepository.contarPorEstado()),
                mapa(usuarioRepository.contarPorRol()),
                mapa(evaluacionRepository.contarPorNivelVigente()),
                mapa(entrenamientoRepository.contarPorEstadoDesde(hace30Dias)),
                mapa(entrenamientoRepository.minutosPorActividadDesde(EstadoValidacion.VALIDADO, hace30Dias)),
                mapa(movimientoRepository.xpPorOrigenDesde(hace30Dias)),
                mapa(participacionRetoRepository.completadosPorRetoDesde(EstadoParticipacion.COMPLETADO, hace30Dias)),
                mapa(participacionTemporadaRepository.contarPorLiga(EstadoTemporada.ACTIVA)),
                mapa(competenciaRepository.contarPorTipo()),
                mapa(notificacionRepository.contarPorEstadoEnvio()));
    }

    private static Map<String, Long> mapa(List<Conteo> conteos) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        conteos.forEach(c -> mapa.put(c.getClave(), c.getTotal()));
        return mapa;
    }
}
