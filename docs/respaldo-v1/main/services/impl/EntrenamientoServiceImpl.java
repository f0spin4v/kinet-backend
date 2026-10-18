package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.RegistrarEntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.ResultadoEntrenamientoDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.mappers.EntrenamientoMapper;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.TipoActividadRepository;
import co.edu.uniquindio.kinet.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orquesta el Proceso 2: registrar sesión → validar (anti-trampa) → calcular XP → actualizar racha →
 * avanzar retos → notificar.
 */
@Service
@RequiredArgsConstructor
public class EntrenamientoServiceImpl implements EntrenamientoService {

    private final EntrenamientoRepository entrenamientoRepository;
    private final TipoActividadRepository actividadRepository;
    private final UsuarioService usuarioService;
    private final AntiTrampaService antiTrampaService;
    private final ProgresoService progresoService;
    private final RetoService retoService;
    private final NotificacionService notificacionService;
    private final EntrenamientoMapper entrenamientoMapper;

    @Override
    @Transactional
    public ResultadoEntrenamientoDTO registrar(String correo, RegistrarEntrenamientoDTO registrarDTO) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        TipoActividad actividad = actividadRepository
                .findByCodigo(registrarDTO.codigoActividad().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("La actividad no existe."));

        int minutos = registrarDTO.duracionMinutos();
        Optional<String> motivoRechazo = antiTrampaService.validar(usuario, registrarDTO.fechaInicio(), minutos);

        Entrenamiento entrenamiento = Entrenamiento.builder()
                .usuario(usuario)
                .tipoActividad(actividad)
                .duracionMinutos(minutos)
                .intensidad(registrarDTO.intensidad())
                .fechaInicio(registrarDTO.fechaInicio())
                .fechaFin(registrarDTO.fechaInicio().plusMinutes(minutos))
                .estado(motivoRechazo.isPresent() ? EstadoEntrenamiento.RECHAZADO : EstadoEntrenamiento.VALIDADO)
                .motivoRechazo(motivoRechazo.orElse(null))
                .build();
        ProgresoJugador progreso = progresoService.obtenerOCrear(usuario);

        if (motivoRechazo.isPresent()) {
            entrenamientoRepository.save(entrenamiento);
            notificacionService.notificar(usuario, TipoNotificacion.ENTRENAMIENTO_RECHAZADO,
                    "Tu entrenamiento no fue validado: " + motivoRechazo.get());
            return new ResultadoEntrenamientoDTO(entrenamientoMapper.toEntrenamientoDTO(entrenamiento), 0,
                    progreso.getXpTotal(), progreso.getNivel(), false, progresoService.rachaVigente(progreso), List.of());
        }

        int xp = ReglasJuego.calcularXp(minutos, actividad.getXpPorMinuto(), registrarDTO.intensidad());
        entrenamiento.setXpOtorgado(xp);
        entrenamientoRepository.save(entrenamiento);

        progresoService.actualizarRacha(progreso, entrenamiento.getFechaInicio().toLocalDate());
        List<ParticipacionReto> completados = retoService.avanzarRetos(usuario, entrenamiento);
        int xpRetos = completados.stream().mapToInt(p -> p.getReto().getXpRecompensa()).sum();

        notificacionService.notificar(usuario, TipoNotificacion.ENTRENAMIENTO_VALIDADO,
                "Entrenamiento validado: " + actividad.getNombre() + ", " + minutos + " min. +" + xp + " XP.");
        boolean subioNivel = progresoService.sumarXp(progreso, xp + xpRetos);

        return new ResultadoEntrenamientoDTO(
                entrenamientoMapper.toEntrenamientoDTO(entrenamiento),
                xp + xpRetos,
                progreso.getXpTotal(),
                progreso.getNivel(),
                subioNivel,
                progreso.getRachaActual(),
                completados.stream().map(p -> p.getReto().getTitulo()).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntrenamientoDTO> historial(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return entrenamientoRepository.findTop50ByUsuarioIdOrderByFechaInicioDesc(usuario.getId()).stream()
                .map(entrenamientoMapper::toEntrenamientoDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntrenamientoDTO obtener(String correo, Long idEntrenamiento) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return entrenamientoRepository.findByIdAndUsuarioId(idEntrenamiento, usuario.getId())
                .map(entrenamientoMapper::toEntrenamientoDTO)
                .orElseThrow(() -> new NoEncontradoException("El entrenamiento no existe."));
    }
}
