package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.progreso.AjusteXpDTO;
import co.edu.uniquindio.kinet.dto.progreso.MovimientoXpDTO;
import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.model.enums.OrigenXp;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.MovimientoXpRepository;
import co.edu.uniquindio.kinet.repositories.PerfilJugadorRepository;
import co.edu.uniquindio.kinet.repositories.TemporadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Libro de XP, nivel y racha del jugador. Todo XP entra por aquí: se registra el movimiento con su origen y se
 * actualiza el saldo (PERFIL_JUGADOR.xp_total) en la misma transacción, así nunca se desfasan.
 */
@Service
@RequiredArgsConstructor
public class ProgresoService {

    private final PerfilJugadorRepository perfilRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final TemporadaRepository temporadaRepository;
    private final CatalogoService catalogoService;
    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    /** Efecto de un movimiento de XP sobre el jugador. */
    public record CambioXp(int cantidad, int xpTotal, int nivelAnterior, int nivelNuevo, MovimientoXp movimiento) {
        public boolean subioNivel() {
            return nivelNuevo > nivelAnterior;
        }
    }

    @Transactional
    public CambioXp porEntrenamiento(Usuario usuario, Entrenamiento entrenamiento, int xp) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.ENTRENAMIENTO).entrenamiento(entrenamiento),
                xp, null);
    }

    @Transactional
    public CambioXp porReto(Usuario usuario, ParticipacionReto participacion, int xp) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.RETO).participacionReto(participacion), xp, null);
    }

    @Transactional
    public CambioXp porLogro(Usuario usuario, Logro logro, int xp) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.LOGRO).logro(logro), xp, null);
    }

    @Transactional
    public CambioXp porEvidencia(Usuario usuario, EvidenciaEntrenamiento evidencia, int xp) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.EVIDENCIA).evidencia(evidencia), xp, null);
    }

    @Transactional
    public CambioXp porCompetencia(Usuario usuario, Competencia competencia, int xp) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.COMPETENCIA).competencia(competencia), xp, null);
    }

    /** Ajuste hecho por el sistema (por ejemplo, revertir el bonus de una evidencia rechazada). */
    @Transactional
    public CambioXp ajusteDelSistema(Usuario usuario, int cantidad, String motivo) {
        return registrar(usuario, MovimientoXp.builder().origen(OrigenXp.AJUSTE), cantidad, motivo);
    }

    /** Corrección manual hecha por soporte. Puede restar, pero el saldo nunca queda negativo. */
    @Transactional
    public MovimientoXpDTO ajustar(AjusteXpDTO ajusteDTO) {
        if (ajusteDTO.cantidad() == 0) {
            throw new ReglaNegocioException("El ajuste debe sumar o restar XP.");
        }
        Usuario usuario = usuarioService.obtenerPorId(ajusteDTO.idUsuario());
        CambioXp cambio = registrar(usuario, MovimientoXp.builder().origen(OrigenXp.AJUSTE), ajusteDTO.cantidad(),
                ajusteDTO.motivo().trim());
        notificacionService.notificar(usuario, TipoNotificacion.NIVEL, "Ajuste de XP",
                (ajusteDTO.cantidad() > 0 ? "+" : "") + ajusteDTO.cantidad() + " XP: " + ajusteDTO.motivo().trim());
        return MovimientoXpDTO.de(cambio.movimiento());
    }

    private CambioXp registrar(Usuario usuario, MovimientoXp.MovimientoXpBuilder movimiento, int cantidad, String motivo) {
        PerfilJugador perfil = usuarioService.perfilDe(usuario);
        int antes = perfil.getXpTotal();
        int despues = antes + cantidad;
        if (despues < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el XP del jugador por debajo de 0 (tiene " + antes + ").");
        }
        Temporada activa = temporadaRepository.findFirstByEstado(EstadoTemporada.ACTIVA).orElse(null);
        MovimientoXp guardado = movimientoRepository.save(movimiento.usuario(usuario).temporada(activa)
                .cantidad(cantidad).motivo(motivo).fechaMovimiento(LocalDateTime.now()).build());
        perfil.setXpTotal(despues);
        perfilRepository.save(perfil);

        int nivelAnterior = catalogoService.nivelPara(antes);
        int nivelNuevo = catalogoService.nivelPara(despues);
        if (nivelNuevo > nivelAnterior) {
            notificacionService.notificar(usuario, TipoNotificacion.NIVEL, "¡Subiste de nivel!",
                    "Llegaste al nivel " + nivelNuevo + ". Sigue así.");
        }
        return new CambioXp(cantidad, despues, nivelAnterior, nivelNuevo, guardado);
    }

    /** Actualiza la racha con un entrenamiento validado del día dado y devuelve la racha nueva. */
    @Transactional
    public int actualizarRacha(Usuario usuario, LocalDate dia) {
        PerfilJugador perfil = usuarioService.perfilDe(usuario);
        int anterior = perfil.getRachaActual();
        int racha = ReglasJuego.nuevaRacha(anterior, perfil.getFechaUltimaActividad(), dia);
        perfil.setRachaActual(racha);
        perfil.setRachaMaxima(Math.max(perfil.getRachaMaxima(), racha));
        if (perfil.getFechaUltimaActividad() == null || dia.isAfter(perfil.getFechaUltimaActividad())) {
            perfil.setFechaUltimaActividad(dia);
        }
        perfilRepository.save(perfil);
        if (racha > anterior && racha >= ReglasJuego.RACHA_AVISO) {
            notificacionService.notificar(usuario, TipoNotificacion.RACHA, "¡Racha de " + racha + " días!",
                    "Llevas " + racha + " días seguidos entrenando.");
        }
        return racha;
    }

    public int rachaVigente(PerfilJugador perfil) {
        return ReglasJuego.rachaVigente(perfil.getRachaActual(), perfil.getFechaUltimaActividad(), LocalDate.now());
    }

    /** Dashboard (UI-003) en una sola consulta. */
    @Transactional(readOnly = true)
    public ProgresoDTO resumen(String correo) {
        LocalDateTime ahora = LocalDateTime.now();
        PerfilJugadorRepository.Resumen r = perfilRepository
                .resumen(UsuarioService.normalizar(correo), LocalDate.now().minusDays(6).atStartOfDay(), ahora)
                .orElseThrow(() -> new NoEncontradoException("El usuario no tiene perfil de jugador."));
        int nivel = r.getNivel() == null ? 1 : r.getNivel();
        int inicio = r.getXpInicioNivel() == null ? 0 : r.getXpInicioNivel();
        Integer siguiente = r.getXpSiguienteNivel();
        int porcentaje = siguiente == null ? 100 : (r.getXpTotal() - inicio) * 100 / (siguiente - inicio);
        String rango = r.getLiga() == null ? null
                : r.getDivision() == null ? r.getLiga() : r.getLiga() + " " + r.getDivision();
        return new ProgresoDTO(
                r.getGamertag(),
                r.getXpTotal(),
                nivel,
                inicio,
                siguiente,
                porcentaje,
                ReglasJuego.rachaVigente(r.getRachaActual(), r.getFechaUltimaActividad(), LocalDate.now()),
                r.getRachaMaxima(),
                r.getEntrenamientosSemana(),
                r.getMinutosSemana(),
                r.getRetosActivos(),
                r.getRetosCompletados(),
                r.getLogros(),
                r.getNotificacionesSinLeer(),
                r.getNivelHabilidad(),
                r.getTemporada(),
                rango,
                r.getPuntosTemporada());
    }

    @Transactional(readOnly = true)
    public PaginaDTO<MovimientoXpDTO> movimientos(String correo, Pageable pagina) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return PaginaDTO.de(movimientoRepository.ultimos(usuario.getId(), pagina));
    }
}
