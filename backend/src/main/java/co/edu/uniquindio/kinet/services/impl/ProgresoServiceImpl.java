package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.ProgresoJugador;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoEntrenamiento;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.*;
import co.edu.uniquindio.kinet.services.NotificacionService;
import co.edu.uniquindio.kinet.services.ProgresoService;
import co.edu.uniquindio.kinet.services.ReglasJuego;
import co.edu.uniquindio.kinet.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgresoServiceImpl implements ProgresoService {

    private final ProgresoJugadorRepository progresoRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final ParticipacionRetoRepository participacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final EvaluacionNivelRepository evaluacionRepository;
    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    @Override
    @Transactional
    public ProgresoJugador obtenerOCrear(Usuario usuario) {
        return progresoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> progresoRepository.save(ProgresoJugador.builder().usuario(usuario).build()));
    }

    @Override
    @Transactional
    public void actualizarRacha(ProgresoJugador progreso, LocalDate diaEntrenamiento) {
        int racha = ReglasJuego.nuevaRacha(progreso.getRachaActual(), progreso.getFechaUltimoEntrenamiento(),
                diaEntrenamiento);
        progreso.setRachaActual(racha);
        progreso.setRachaMaxima(Math.max(progreso.getRachaMaxima(), racha));
        LocalDate ultimo = progreso.getFechaUltimoEntrenamiento();
        if (ultimo == null || diaEntrenamiento.isAfter(ultimo)) {
            progreso.setFechaUltimoEntrenamiento(diaEntrenamiento);
        }
        progresoRepository.save(progreso);
    }

    @Override
    @Transactional
    public boolean sumarXp(ProgresoJugador progreso, int xp) {
        int nivelAnterior = progreso.getNivel();
        progreso.setXpTotal(progreso.getXpTotal() + xp);
        int nivelNuevo = ReglasJuego.nivelParaXp(progreso.getXpTotal());
        progreso.setNivel(nivelNuevo);
        progresoRepository.save(progreso);
        if (nivelNuevo > nivelAnterior) {
            notificacionService.notificar(progreso.getUsuario(), TipoNotificacion.NIVEL_ALCANZADO,
                    "¡Subiste a nivel " + nivelNuevo + "! Sigue así.");
            return true;
        }
        return false;
    }

    @Override
    public int rachaVigente(ProgresoJugador progreso) {
        return ReglasJuego.rachaVigente(progreso.getRachaActual(), progreso.getFechaUltimoEntrenamiento(),
                LocalDate.now());
    }

    @Override
    @Transactional
    public ProgresoDTO resumen(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        ProgresoJugador progreso = obtenerOCrear(usuario);
        Long id = usuario.getId();

        LocalDateTime ahora = LocalDateTime.now();
        List<Entrenamiento> semana = entrenamientoRepository.findByUsuarioIdAndEstadoAndFechaInicioBetween(
                id, EstadoEntrenamiento.VALIDADO, LocalDate.now().minusDays(6).atStartOfDay(), ahora);

        int nivel = progreso.getNivel();
        int inicioNivel = ReglasJuego.xpParaNivel(nivel);
        int siguienteNivel = ReglasJuego.xpParaNivel(nivel + 1);
        int porcentaje = (progreso.getXpTotal() - inicioNivel) * 100 / (siguienteNivel - inicioNivel);

        String nivelInicial = evaluacionRepository.findByUsuarioId(id)
                .map(e -> e.getNivelInicial().name())
                .orElse(null);

        return new ProgresoDTO(
                progreso.getXpTotal(),
                nivel,
                inicioNivel,
                siguienteNivel,
                porcentaje,
                rachaVigente(progreso),
                progreso.getRachaMaxima(),
                semana.size(),
                semana.stream().mapToInt(Entrenamiento::getDuracionMinutos).sum(),
                participacionRepository.countByUsuarioIdAndEstadoAndFechaLimiteAfter(id, EstadoParticipacion.EN_CURSO, ahora),
                participacionRepository.countByUsuarioIdAndEstado(id, EstadoParticipacion.COMPLETADO),
                notificacionRepository.countByUsuarioIdAndLeida(id, false),
                nivelInicial);
    }
}
