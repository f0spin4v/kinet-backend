package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.clan.*;
import co.edu.uniquindio.kinet.exceptions.AccesoDenegadoException;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.Clan;
import co.edu.uniquindio.kinet.model.MembresiaClan;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoClan;
import co.edu.uniquindio.kinet.model.enums.EstadoParticipacion;
import co.edu.uniquindio.kinet.model.enums.RolClan;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.ClanRepository;
import co.edu.uniquindio.kinet.repositories.MembresiaClanRepository;
import co.edu.uniquindio.kinet.repositories.ParticipacionRetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Clanes: un jugador está en un solo clan a la vez y cada clan tiene un único líder. Si el líder sale, el
 * liderazgo pasa al miembro más antiguo; si sale el último miembro, el clan se cierra.
 */
@Service
@RequiredArgsConstructor
public class ClanService {

    private final ClanRepository clanRepository;
    private final MembresiaClanRepository membresiaRepository;
    private final ParticipacionRetoRepository participacionRepository;
    private final UsuarioService usuarioService;
    private final LogroService logroService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public PaginaDTO<ClanDTO> listar(Pageable pagina) {
        return PaginaDTO.de(clanRepository.resumenes(EstadoClan.ACTIVO, RolClan.LIDER, pagina).map(ClanService::dto));
    }

    @Transactional(readOnly = true)
    public ClanDetalleDTO detalle(Long idClan) {
        return detalle(buscar(idClan));
    }

    @Transactional(readOnly = true)
    public ClanDetalleDTO miClan(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return membresiaRepository.findByUsuarioIdAndFechaSalidaIsNull(usuario.getId())
                .map(m -> detalle(m.getClan()))
                .orElseThrow(() -> new NoEncontradoException("No perteneces a ningún clan."));
    }

    @Transactional
    public ClanDetalleDTO crear(String correo, GuardarClanDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        sinClan(usuario);
        if (clanRepository.existsByNombre(dto.nombre().trim())) {
            throw new ConflictoException("Ya existe un clan con ese nombre.");
        }
        Clan clan = clanRepository.save(Clan.builder()
                .nombre(dto.nombre().trim())
                .descripcion(textoOpcional(dto.descripcion()))
                .capacidadMax(dto.capacidadMax())
                .build());
        membresiaRepository.save(MembresiaClan.builder().clan(clan).usuario(usuario).rolClan(RolClan.LIDER).build());
        logroService.evaluar(usuario);
        return detalle(clan);
    }

    @Transactional
    public ClanDetalleDTO editar(String correo, Long idClan, GuardarClanDTO dto) {
        Clan clan = buscar(idClan);
        exigirLider(correo, clan);
        if (clanRepository.existsByNombreAndIdNot(dto.nombre().trim(), idClan)) {
            throw new ConflictoException("Ya existe un clan con ese nombre.");
        }
        if (dto.capacidadMax() < membresiaRepository.countByClanIdAndFechaSalidaIsNull(idClan)) {
            throw new ReglaNegocioException("La capacidad no puede ser menor que los miembros actuales.");
        }
        clan.setNombre(dto.nombre().trim());
        clan.setDescripcion(textoOpcional(dto.descripcion()));
        clan.setCapacidadMax(dto.capacidadMax());
        return detalle(clanRepository.save(clan));
    }

    @Transactional
    public ClanDetalleDTO unirse(String correo, Long idClan) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Clan clan = buscar(idClan);
        if (clan.getEstado() != EstadoClan.ACTIVO) {
            throw new ReglaNegocioException("Este clan está cerrado.");
        }
        sinClan(usuario);
        if (membresiaRepository.countByClanIdAndFechaSalidaIsNull(idClan) >= clan.getCapacidadMax()) {
            throw new ReglaNegocioException("El clan está lleno.");
        }
        membresiaRepository.save(MembresiaClan.builder().clan(clan).usuario(usuario).rolClan(RolClan.MIEMBRO).build());
        lider(clan).ifPresent(l -> notificacionService.notificar(l.getUsuario(), TipoNotificacion.CLAN,
                "Nuevo miembro", usuarioService.perfilDe(usuario).getGamertag() + " se unió a " + clan.getNombre() + "."));
        logroService.evaluar(usuario);
        return detalle(clan);
    }

    @Transactional
    public void salir(String correo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        MembresiaClan membresia = membresiaRepository.findByUsuarioIdAndFechaSalidaIsNull(usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("No perteneces a ningún clan."));
        Clan clan = membresia.getClan();
        if (membresia.getRolClan() == RolClan.LIDER) {
            Optional<MembresiaClan> siguiente = membresiaRepository
                    .findFirstByClanIdAndFechaSalidaIsNullAndIdNotOrderByFechaIngresoAsc(clan.getId(), membresia.getId());
            if (siguiente.isEmpty()) {
                cerrar(clan, "El clan " + clan.getNombre() + " se cerró porque salió su último miembro.");
                return;
            }
            terminar(membresia);
            MembresiaClan nuevo = siguiente.get();
            nuevo.setRolClan(RolClan.LIDER);
            membresiaRepository.save(nuevo);
            notificacionService.notificar(nuevo.getUsuario(), TipoNotificacion.CLAN, "Eres el nuevo líder",
                    "Ahora lideras " + clan.getNombre() + ".");
            return;
        }
        terminar(membresia);
    }

    @Transactional
    public void expulsar(String correo, Long idClan, Long idUsuario) {
        Clan clan = buscar(idClan);
        MembresiaClan lider = exigirLider(correo, clan);
        if (lider.getUsuario().getId().equals(idUsuario)) {
            throw new ReglaNegocioException("No puedes expulsarte: usa la opción de salir del clan.");
        }
        MembresiaClan miembro = miembroVigente(clan, idUsuario);
        terminar(miembro);
        notificacionService.notificar(miembro.getUsuario(), TipoNotificacion.CLAN, "Saliste del clan",
                "El líder te retiró de " + clan.getNombre() + ".");
    }

    @Transactional
    public ClanDetalleDTO transferirLiderazgo(String correo, Long idClan, Long idNuevoLider) {
        Clan clan = buscar(idClan);
        MembresiaClan lider = exigirLider(correo, clan);
        MembresiaClan nuevo = miembroVigente(clan, idNuevoLider);
        if (nuevo.getId().equals(lider.getId())) {
            throw new ReglaNegocioException("Ya eres el líder del clan.");
        }
        // primero se baja el líder actual: el índice único no permite dos líderes ni por un instante
        lider.setRolClan(RolClan.MIEMBRO);
        membresiaRepository.saveAndFlush(lider);
        nuevo.setRolClan(RolClan.LIDER);
        membresiaRepository.save(nuevo);
        notificacionService.notificar(nuevo.getUsuario(), TipoNotificacion.CLAN, "Eres el nuevo líder",
                "Ahora lideras " + clan.getNombre() + ".");
        return detalle(clan);
    }

    /** Cierre por el líder o por moderación. */
    @Transactional
    public void cerrar(String correo, Long idClan, boolean moderacion) {
        Clan clan = buscar(idClan);
        if (clan.getEstado() == EstadoClan.CERRADO) {
            throw new ReglaNegocioException("El clan ya está cerrado.");
        }
        if (!moderacion) {
            exigirLider(correo, clan);
        }
        cerrar(clan, moderacion ? "Moderación cerró el clan " + clan.getNombre() + "."
                : "El líder cerró el clan " + clan.getNombre() + ".");
    }

    // ---------- apoyo ----------

    /** Cierra el clan: termina todas las membresías y abandona sus retos de clan en curso (en bloque). */
    private void cerrar(Clan clan, String mensaje) {
        List<MembresiaClan> miembros = membresiaRepository.findByClanIdAndFechaSalidaIsNullOrderByFechaIngresoAsc(clan.getId());
        membresiaRepository.terminarTodas(clan.getId(), LocalDateTime.now());
        participacionRepository.cambiarEstadoDeClan(clan.getId(), EstadoParticipacion.EN_CURSO,
                EstadoParticipacion.ABANDONADO);
        miembros.forEach(m -> notificacionService.notificar(m.getUsuario(), TipoNotificacion.CLAN, "Clan cerrado", mensaje));
        clan.setEstado(EstadoClan.CERRADO);
        clanRepository.save(clan);
    }

    /** Termina la membresía y abandona los retos de clan que el jugador tenía en curso con ese clan. */
    private void terminar(MembresiaClan membresia) {
        membresia.setFechaSalida(LocalDateTime.now());
        membresiaRepository.saveAndFlush(membresia);
        participacionRepository.cambiarEstadoDeMiembro(membresia.getClan().getId(), membresia.getUsuario().getId(),
                EstadoParticipacion.EN_CURSO, EstadoParticipacion.ABANDONADO);
    }

    private void sinClan(Usuario usuario) {
        if (membresiaRepository.existsByUsuarioIdAndFechaSalidaIsNull(usuario.getId())) {
            throw new ConflictoException("Ya perteneces a un clan. Sal de él primero.");
        }
    }

    private MembresiaClan exigirLider(String correo, Clan clan) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        return lider(clan)
                .filter(l -> l.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow(() -> new AccesoDenegadoException("Solo el líder del clan puede hacer esto."));
    }

    private Optional<MembresiaClan> lider(Clan clan) {
        return membresiaRepository.findFirstByClanIdAndRolClanAndFechaSalidaIsNull(clan.getId(), RolClan.LIDER);
    }

    private MembresiaClan miembroVigente(Clan clan, Long idUsuario) {
        return membresiaRepository.findByUsuarioIdAndClanIdAndFechaSalidaIsNull(idUsuario, clan.getId())
                .orElseThrow(() -> new NoEncontradoException("Ese jugador no es miembro del clan."));
    }

    private static ClanDTO dto(ClanRepository.Resumen r) {
        return new ClanDTO(r.getId(), r.getNombre(), r.getDescripcion(), r.getCapacidadMax(), r.getMiembros(),
                r.getLider(), r.getEstado().name(), r.getFechaCreacion());
    }

    /** Resumen del clan y sus miembros: dos consultas, sin importar cuántos miembros tenga. */
    private ClanDetalleDTO detalle(Clan clan) {
        ClanRepository.Resumen resumen = clanRepository.resumen(clan.getId(), RolClan.LIDER).orElseThrow();
        return new ClanDetalleDTO(dto(resumen), membresiaRepository.miembros(clan.getId()));
    }

    private Clan buscar(Long idClan) {
        return clanRepository.findById(idClan).orElseThrow(() -> new NoEncontradoException("El clan no existe."));
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
