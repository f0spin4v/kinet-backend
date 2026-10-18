package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.PaginaDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.EvidenciaDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.RevisionEvidenciaDTO;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.EvidenciaEntrenamiento;
import co.edu.uniquindio.kinet.model.MovimientoXp;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoEvidencia;
import co.edu.uniquindio.kinet.model.enums.EstadoValidacion;
import co.edu.uniquindio.kinet.model.enums.TipoNotificacion;
import co.edu.uniquindio.kinet.repositories.EntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.EvidenciaEntrenamientoRepository;
import co.edu.uniquindio.kinet.repositories.MovimientoXpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Evidencia de un entrenamiento: una imagen (captura del reloj o de una app) o el enlace a la actividad en una app
 * deportiva reconocida. Da un pequeño bonus de XP. Es opcional, salvo el enlace en las sesiones de más de 180
 * minutos y para que un entrenamiento cuente en enfrentamientos y competencias. No se valida automáticamente;
 * queda SIN_REVISAR como respaldo por si el jugador es reportado o un entrenador quiere revisarla. Si un
 * entrenador la rechaza, el bonus se revierte.
 */
@Service
@RequiredArgsConstructor
public class EvidenciaService {

    private final EvidenciaEntrenamientoRepository evidenciaRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final MovimientoXpRepository movimientoRepository;
    private final UsuarioService usuarioService;
    private final ProgresoService progresoService;
    private final NotificacionService notificacionService;
    private final EnlaceDeporte enlaceDeporte;
    private final Clock reloj;

    /** Archivo para descargar. */
    public record Archivo(String nombre, String tipoContenido, byte[] contenido) {
    }

    /**
     * Sube una imagen como evidencia de un entrenamiento validado, una sola vez y dentro de las 48 horas siguientes.
     * Una sesión pendiente (más de 180 minutos) necesita el enlace de la actividad, no una imagen.
     */
    @Transactional
    public EvidenciaDTO subir(String correo, Long idEntrenamiento, MultipartFile archivo) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Entrenamiento entrenamiento = evidenciable(usuario, idEntrenamiento);
        if (entrenamiento.getEstadoValidacion() == EstadoValidacion.PENDIENTE) {
            throw new ReglaNegocioException("Una sesión de más de " + ReglasJuego.UMBRAL_REVISION
                    + " minutos necesita el enlace de la actividad en una app deportiva, no una imagen.");
        }
        if (evidenciaRepository.existsByEntrenamientoId(idEntrenamiento)) {
            throw new ConflictoException("Este entrenamiento ya tiene evidencia.");
        }
        byte[] contenido = leer(archivo);
        EvidenciaEntrenamiento evidencia = evidenciaRepository.save(EvidenciaEntrenamiento.builder()
                .entrenamiento(entrenamiento)
                .nombreArchivo(nombre(archivo))
                .tipoContenido(tipoImagen(contenido))
                .tamanoBytes(contenido.length)
                .contenido(contenido)
                .fechaSubida(LocalDateTime.now(reloj))
                .build());
        otorgarBonus(usuario, evidencia);
        return datos(evidencia.getId());
    }

    /**
     * Sube el enlace a la actividad en una app deportiva reconocida (tabla PLATAFORMA_DEPORTE). Un mismo enlace no
     * sirve para dos entrenamientos. Una sesión validada tiene 48 horas; una pendiente, 4 horas desde que terminó.
     * Si el entrenamiento ya tenía una imagen (sin rechazar), el enlace la reemplaza sin repetir el bonus. Si está
     * validado, el bonus se da enseguida; si está pendiente, al aprobarlo.
     */
    @Transactional
    public EvidenciaDTO subirEnlace(String correo, Long idEntrenamiento, String url) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        Entrenamiento entrenamiento = evidenciable(usuario, idEntrenamiento);
        EnlaceDeporte.Reconocido enlace = enlaceDeporte.reconocer(url);
        EvidenciaEntrenamiento evidencia = evidenciaRepository.findByEntrenamientoId(idEntrenamiento).orElse(null);
        if (evidencia != null && (evidencia.esEnlace() || evidencia.getEstadoRevision() == EstadoEvidencia.RECHAZADA)) {
            throw new ConflictoException("Este entrenamiento ya tiene evidencia.");
        }
        if (evidenciaRepository.existsByUrlExterna(enlace.url())) {
            throw new ConflictoException("Ese enlace ya se usó en otro entrenamiento.");
        }
        if (evidencia == null) {
            evidencia = EvidenciaEntrenamiento.builder().entrenamiento(entrenamiento).build();
        } else {
            evidencia.setNombreArchivo(null);
            evidencia.setTipoContenido(null);
            evidencia.setTamanoBytes(null);
            evidencia.setContenido(null);
            evidencia.setEstadoRevision(EstadoEvidencia.SIN_REVISAR);
            evidencia.setNotaRevision(null);
            evidencia.setFechaRevision(null);
        }
        evidencia.setUrlExterna(enlace.url());
        evidencia.setPlataforma(enlace.plataforma());
        evidencia.setFechaSubida(LocalDateTime.now(reloj));
        evidencia = evidenciaRepository.save(evidencia);
        otorgarBonus(usuario, evidencia);
        return datos(evidencia.getId());
    }

    /** ¿El entrenamiento tiene un enlace de evidencia que no fue rechazado? */
    @Transactional(readOnly = true)
    public boolean tieneEnlaceVigente(Long idEntrenamiento) {
        return evidenciaRepository.existsByEntrenamientoIdAndUrlExternaIsNotNullAndEstadoRevisionNot(idEntrenamiento,
                EstadoEvidencia.RECHAZADA);
    }

    /** Da el bonus si la evidencia existe y todavía no lo dio. Se llama al aprobar un entrenamiento pendiente. */
    @Transactional
    public void otorgarBonusSiCorresponde(Entrenamiento entrenamiento) {
        evidenciaRepository.findByEntrenamientoId(entrenamiento.getId())
                .filter(e -> e.getEstadoRevision() != EstadoEvidencia.RECHAZADA)
                .filter(e -> !movimientoRepository.existsByEvidenciaId(e.getId()))
                .ifPresent(e -> otorgarBonus(entrenamiento.getUsuario(), e));
    }

    @Transactional(readOnly = true)
    public Archivo archivoPropio(String correo, Long idEntrenamiento) {
        Usuario usuario = usuarioService.obtenerEntidad(correo);
        entrenamientoRepository.findByIdAndUsuarioId(idEntrenamiento, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("El entrenamiento no existe."));
        return archivo(evidenciaRepository.findByEntrenamientoId(idEntrenamiento)
                .orElseThrow(() -> new NoEncontradoException("Este entrenamiento no tiene evidencia.")));
    }

    /** Estado de la evidencia de cada entrenamiento (sin cargar las imágenes). */
    @Transactional(readOnly = true)
    public Map<Long, String> estados(Collection<Long> idsEntrenamiento) {
        Map<Long, String> estados = new HashMap<>();
        if (!idsEntrenamiento.isEmpty()) {
            evidenciaRepository.estadosDe(idsEntrenamiento)
                    .forEach(fila -> estados.put((Long) fila[0], ((EstadoEvidencia) fila[1]).name()));
        }
        return estados;
    }

    // ---------- moderación ----------

    @Transactional(readOnly = true)
    public PaginaDTO<EvidenciaDTO> listar(EstadoEvidencia estado, Pageable pagina) {
        return PaginaDTO.de(evidenciaRepository.datos(estado, pagina));
    }

    @Transactional(readOnly = true)
    public Archivo archivo(Long idEvidencia) {
        return archivo(buscar(idEvidencia));
    }

    /** ACEPTADA no cambia nada. RECHAZADA exige nota y revierte el bonus (sin dejar el XP en negativo). */
    @Transactional
    public EvidenciaDTO revisar(Long idEvidencia, RevisionEvidenciaDTO revision) {
        EvidenciaEntrenamiento evidencia = buscar(idEvidencia);
        if (evidencia.getEstadoRevision() != EstadoEvidencia.SIN_REVISAR) {
            throw new ReglaNegocioException("Esta evidencia ya fue revisada.");
        }
        Usuario jugador = evidencia.getEntrenamiento().getUsuario();
        evidencia.setFechaRevision(LocalDateTime.now(reloj));
        if (Boolean.TRUE.equals(revision.aceptar())) {
            evidencia.setEstadoRevision(EstadoEvidencia.ACEPTADA);
            evidencia.setNotaRevision(textoOpcional(revision.nota()));
        } else {
            String nota = textoOpcional(revision.nota());
            if (nota == null) {
                throw new ReglaNegocioException("Escribe el motivo del rechazo.");
            }
            evidencia.setEstadoRevision(EstadoEvidencia.RECHAZADA);
            evidencia.setNotaRevision(nota);
            movimientoRepository.findByEvidenciaId(evidencia.getId()).map(MovimientoXp::getCantidad).ifPresent(bonus -> {
                int saldo = usuarioService.perfilDe(jugador).getXpTotal();
                int revertir = Math.min(bonus, saldo);
                if (revertir > 0) {
                    progresoService.ajusteDelSistema(jugador, -revertir, "Evidencia rechazada: " + nota);
                }
            });
            notificacionService.notificar(jugador, TipoNotificacion.VALIDACION, "Evidencia rechazada",
                    "Un entrenador rechazó la evidencia de tu entrenamiento: " + nota);
        }
        evidenciaRepository.save(evidencia);
        return datos(idEvidencia);
    }

    // ---------- apoyo ----------

    /** Un entrenamiento del jugador, terminado, no rechazado y dentro del plazo para subirle evidencia. */
    private Entrenamiento evidenciable(Usuario usuario, Long idEntrenamiento) {
        Entrenamiento entrenamiento = entrenamientoRepository.findByIdAndUsuarioId(idEntrenamiento, usuario.getId())
                .orElseThrow(() -> new NoEncontradoException("El entrenamiento no existe."));
        if (entrenamiento.getEstadoValidacion() != EstadoValidacion.VALIDADO
                && entrenamiento.getEstadoValidacion() != EstadoValidacion.PENDIENTE) {
            throw new ReglaNegocioException("Solo puedes subir evidencia de un entrenamiento terminado que no fue rechazado.");
        }
        int horas = entrenamiento.getEstadoValidacion() == EstadoValidacion.PENDIENTE
                ? ReglasJuego.HORAS_EVIDENCIA_SESION_LARGA : ReglasJuego.HORAS_PARA_EVIDENCIA;
        if (entrenamiento.getFechaHoraFin().plusHours(horas).isBefore(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("La evidencia se sube en las " + horas
                    + " horas siguientes al entrenamiento.");
        }
        return entrenamiento;
    }

    /** Da el bonus si el entrenamiento ya está validado y la evidencia todavía no lo recibió. */
    private void otorgarBonus(Usuario usuario, EvidenciaEntrenamiento evidencia) {
        if (evidencia.getEntrenamiento().getEstadoValidacion() != EstadoValidacion.VALIDADO
                || movimientoRepository.existsByEvidenciaId(evidencia.getId())) {
            return;
        }
        progresoService.porEvidencia(usuario, evidencia, ReglasJuego.BONUS_EVIDENCIA);
        notificacionService.notificar(usuario, TipoNotificacion.VALIDACION, "Evidencia recibida",
                "Gracias por la evidencia de tu entrenamiento. +" + ReglasJuego.BONUS_EVIDENCIA + " XP.");
    }

    private EvidenciaDTO datos(Long idEvidencia) {
        return evidenciaRepository.datosPorId(idEvidencia).orElseThrow();
    }

    private EvidenciaEntrenamiento buscar(Long idEvidencia) {
        return evidenciaRepository.findById(idEvidencia)
                .orElseThrow(() -> new NoEncontradoException("La evidencia no existe."));
    }

    private static Archivo archivo(EvidenciaEntrenamiento e) {
        if (e.esEnlace()) {
            throw new NoEncontradoException("Esta evidencia es un enlace, no una imagen: ábrelo en " + e.getUrlExterna());
        }
        return new Archivo(e.getNombreArchivo(), e.getTipoContenido(), e.getContenido());
    }

    private static byte[] leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Adjunta una imagen.");
        }
        if (archivo.getSize() > ReglasJuego.TAMANO_MAXIMO_EVIDENCIA) {
            throw new ReglaNegocioException("La imagen no puede pesar más de 5 MB.");
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer la imagen. Intenta de nuevo.");
        }
    }

    /** El tipo se decide por los primeros bytes del archivo, no por lo que diga el navegador. */
    static String tipoImagen(byte[] b) {
        if (empiezaCon(b, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (empiezaCon(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (b.length >= 12 && empiezaCon(b, 'R', 'I', 'F', 'F')
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new ReglaNegocioException("La evidencia debe ser una imagen JPEG, PNG o WEBP.");
    }

    private static boolean empiezaCon(byte[] b, int... firma) {
        if (b.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((b[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static String nombre(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "evidencia";
        }
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        return nombre.length() > 200 ? nombre.substring(nombre.length() - 200) : nombre;
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
