package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.model.ProgresoJugador;
import co.edu.uniquindio.kinet.model.Usuario;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProgresoServiceImpl implements ProgresoService {

    private final ProgresoJugadorRepository progresoRepository;
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
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioSemana = LocalDate.now().minusDays(6).atStartOfDay();

        ProgresoJugadorRepository.Resumen resumen = progresoRepository.resumen(correoNormalizado, inicioSemana, ahora)
                .orElseThrow(() -> new NoEncontradoException("El usuario no existe."));
        if (resumen.getXpTotal() == null) {
            // Cuentas creadas antes del Proceso 2 todavía no tienen progreso: se crea y se vuelve a consultar.
            obtenerOCrear(usuarioService.obtenerEntidad(correoNormalizado));
            resumen = progresoRepository.resumen(correoNormalizado, inicioSemana, ahora).orElseThrow();
        }

        int nivel = resumen.getNivel();
        int inicioNivel = ReglasJuego.xpParaNivel(nivel);
        int siguienteNivel = ReglasJuego.xpParaNivel(nivel + 1);
        int porcentaje = (resumen.getXpTotal() - inicioNivel) * 100 / (siguienteNivel - inicioNivel);

        return new ProgresoDTO(
                resumen.getXpTotal(),
                nivel,
                inicioNivel,
                siguienteNivel,
                porcentaje,
                ReglasJuego.rachaVigente(resumen.getRachaActual(), resumen.getFechaUltimoEntrenamiento(), LocalDate.now()),
                resumen.getRachaMaxima(),
                resumen.getEntrenamientosSemana().intValue(),
                resumen.getMinutosSemana().intValue(),
                resumen.getRetosActivos(),
                resumen.getRetosCompletados(),
                resumen.getNotificacionesSinLeer(),
                resumen.getNivelInicial() != null ? resumen.getNivelInicial().name() : null);
    }
}
