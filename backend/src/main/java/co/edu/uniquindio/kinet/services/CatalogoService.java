package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.catalogo.*;
import co.edu.uniquindio.kinet.exceptions.ConflictoException;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.exceptions.ReglaNegocioException;
import co.edu.uniquindio.kinet.model.*;
import co.edu.uniquindio.kinet.model.enums.EstadoActividad;
import co.edu.uniquindio.kinet.model.enums.Intensidad;
import co.edu.uniquindio.kinet.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catálogos que configura el administrador: actividades, reglas de puntuación, niveles de progresión,
 * niveles de habilidad, ligas y rangos.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final TipoActividadRepository actividadRepository;
    private final ReglaPuntuacionRepository reglaRepository;
    private final NivelProgresionRepository nivelProgresionRepository;
    private final NivelHabilidadRepository nivelHabilidadRepository;
    private final LigaRepository ligaRepository;
    private final RangoRepository rangoRepository;

    // ---------- actividades y reglas ----------

    /** Actividades ACTIVAS con su XP por minuto vigente hoy (lo que ve el jugador). */
    @Transactional(readOnly = true)
    public List<ActividadDTO> actividades() {
        return actividades(false);
    }

    /** Actividades con su XP por minuto vigente hoy para cada intensidad; todas incluye las INACTIVAS. */
    @Transactional(readOnly = true)
    public List<ActividadDTO> actividades(boolean todas) {
        LocalDate hoy = LocalDate.now();
        Map<Long, List<ReglaDTO>> reglas = reglaRepository.vigentesEn(hoy).stream()
                .sorted(Comparator.comparing(ReglaPuntuacion::getIntensidad))
                .map(ReglaDTO::de)
                .collect(Collectors.groupingBy(ReglaDTO::idTipoActividad));
        List<TipoActividad> actividades = todas ? actividadRepository.findAllByOrderByNombreAsc()
                : actividadRepository.findByEstadoOrderByNombreAsc(EstadoActividad.ACTIVA);
        return actividades.stream()
                .map(a -> ActividadDTO.de(a, reglas.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoActividad actividad(Long idActividad) {
        return actividadRepository.findById(idActividad)
                .orElseThrow(() -> new NoEncontradoException("La actividad no existe."));
    }

    /** Actividad que se puede usar para entrenar o competir: tiene que estar ACTIVA. */
    @Transactional(readOnly = true)
    public TipoActividad actividadActiva(Long idActividad) {
        TipoActividad actividad = actividad(idActividad);
        if (actividad.getEstado() != EstadoActividad.ACTIVA) {
            throw new ReglaNegocioException("La actividad " + actividad.getNombre() + " no está disponible.");
        }
        return actividad;
    }

    /** INACTIVA: deja de aparecer para el jugador y no se puede usar; su historial se conserva. */
    @Transactional
    public ActividadDTO cambiarEstadoActividad(Long idActividad, EstadoActividad estado) {
        TipoActividad actividad = actividad(idActividad);
        actividad.setEstado(estado);
        actividadRepository.save(actividad);
        return actividades(true).stream().filter(a -> a.id().equals(idActividad)).findFirst().orElseThrow();
    }

    @Transactional
    public ActividadDTO crearActividad(GuardarActividadDTO dto) {
        String nombre = nombreActividad(dto.nombre());
        if (actividadRepository.existsByNombre(nombre)) {
            throw new ConflictoException("Ya existe una actividad con ese nombre.");
        }
        TipoActividad actividad = actividadRepository.save(TipoActividad.builder().nombre(nombre).build());
        return ActividadDTO.de(actividad, List.of());
    }

    @Transactional
    public ActividadDTO editarActividad(Long idActividad, GuardarActividadDTO dto) {
        TipoActividad actividad = actividad(idActividad);
        String nombre = nombreActividad(dto.nombre());
        if (actividadRepository.existsByNombreAndIdNot(nombre, idActividad)) {
            throw new ConflictoException("Ya existe una actividad con ese nombre.");
        }
        actividad.setNombre(nombre);
        actividadRepository.save(actividad);
        return actividades(true).stream().filter(a -> a.id().equals(idActividad)).findFirst().orElseThrow();
    }

    /** Historial de reglas de una actividad, o las vigentes de todas si idActividad es null. */
    @Transactional(readOnly = true)
    public List<ReglaDTO> reglas(Long idActividad) {
        List<ReglaPuntuacion> reglas = idActividad == null
                ? reglaRepository.findByVigenteHastaIsNullOrderByTipoActividadNombreAscIntensidadAsc()
                : reglaRepository.findByTipoActividadIdOrderByIntensidadAscVigenteDesdeDesc(idActividad);
        return reglas.stream().map(ReglaDTO::de).toList();
    }

    /**
     * Crea una regla nueva. La que estaba abierta para esa actividad e intensidad se cierra el día anterior,
     * así nunca hay dos reglas vigentes a la vez. No se pueden crear reglas con fecha pasada: el XP ya otorgado
     * no se recalcula.
     */
    @Transactional
    public ReglaDTO crearRegla(CrearReglaDTO dto) {
        TipoActividad actividad = actividad(dto.idTipoActividad());
        if (dto.vigenteDesde().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La regla no puede empezar en una fecha pasada.");
        }
        reglaRepository.findByTipoActividadIdAndIntensidadAndVigenteHastaIsNull(actividad.getId(), dto.intensidad())
                .ifPresent(abierta -> {
                    if (!abierta.getVigenteDesde().isBefore(dto.vigenteDesde())) {
                        throw new ConflictoException("Ya hay una regla que empieza el " + abierta.getVigenteDesde()
                                + ". La nueva debe empezar después.");
                    }
                    abierta.setVigenteHasta(dto.vigenteDesde().minusDays(1));
                    reglaRepository.saveAndFlush(abierta);
                });
        ReglaPuntuacion regla = reglaRepository.save(ReglaPuntuacion.builder()
                .tipoActividad(actividad)
                .intensidad(dto.intensidad())
                .xpPorMinuto(dto.xpPorMinuto())
                .duracionMinimaMin(dto.duracionMinimaMin())
                .vigenteDesde(dto.vigenteDesde())
                .build());
        return ReglaDTO.de(regla);
    }

    /** Regla que aplicaba el día del entrenamiento. */
    @Transactional(readOnly = true)
    public ReglaPuntuacion reglaVigente(TipoActividad actividad, Intensidad intensidad, LocalDate fecha) {
        return reglaRepository.vigenteEn(actividad.getId(), intensidad, fecha)
                .orElseThrow(() -> new ReglaNegocioException("La actividad " + actividad.getNombre()
                        + " no tiene una regla de puntuación vigente para intensidad " + intensidad + "."));
    }

    // ---------- niveles ----------

    @Transactional(readOnly = true)
    public List<NivelHabilidadDTO> nivelesHabilidad() {
        return nivelHabilidadRepository.findAllByOrderByOrdenAsc().stream().map(NivelHabilidadDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public List<NivelProgresionDTO> nivelesProgresion() {
        return nivelProgresionRepository.findAllByOrderByNumeroNivelAsc().stream().map(NivelProgresionDTO::de).toList();
    }

    /** Cambia el XP de un nivel manteniendo la tabla creciente. El nivel 1 siempre empieza en 0 XP. */
    @Transactional
    public NivelProgresionDTO editarNivelProgresion(Integer numeroNivel, Integer xpRequerido) {
        NivelProgresion nivel = nivelProgresionRepository.findByNumeroNivel(numeroNivel)
                .orElseThrow(() -> new NoEncontradoException("El nivel no existe."));
        if (numeroNivel == 1 && xpRequerido != 0) {
            throw new ReglaNegocioException("El nivel 1 empieza en 0 XP.");
        }
        nivelProgresionRepository.findByNumeroNivel(numeroNivel - 1)
                .filter(anterior -> xpRequerido <= anterior.getXpRequerido())
                .ifPresent(anterior -> {
                    throw new ReglaNegocioException("Debe pedir más XP que el nivel " + anterior.getNumeroNivel() + ".");
                });
        nivelProgresionRepository.findByNumeroNivel(numeroNivel + 1)
                .filter(siguiente -> xpRequerido >= siguiente.getXpRequerido())
                .ifPresent(siguiente -> {
                    throw new ReglaNegocioException("Debe pedir menos XP que el nivel " + siguiente.getNumeroNivel() + ".");
                });
        nivel.setXpRequerido(xpRequerido);
        return NivelProgresionDTO.de(nivelProgresionRepository.save(nivel));
    }

    /** Nivel que corresponde a un XP acumulado: el de mayor umbral alcanzado (la tabla está en caché). */
    @Transactional(readOnly = true)
    public int nivelPara(int xp) {
        int nivel = 1;
        for (NivelProgresion n : nivelProgresionRepository.findAllByOrderByNumeroNivelAsc()) {
            if (n.getXpRequerido() <= xp) {
                nivel = n.getNumeroNivel();
            }
        }
        return nivel;
    }

    @Transactional(readOnly = true)
    public NivelHabilidad nivelHabilidad(String nombre) {
        return nivelHabilidadRepository.findByNombre(nombre.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("El nivel de habilidad " + nombre + " no existe."));
    }

    @Transactional(readOnly = true)
    public NivelHabilidad nivelHabilidad(Long idNivel) {
        return nivelHabilidadRepository.findById(idNivel)
                .orElseThrow(() -> new NoEncontradoException("El nivel de habilidad no existe."));
    }

    // ---------- ligas y rangos ----------

    @Transactional(readOnly = true)
    public List<LigaDTO> ligas() {
        Map<Long, List<RangoDTO>> rangos = rangoRepository.findAllByOrderByOrdenAsc().stream()
                .collect(Collectors.groupingBy(r -> r.getLiga().getId(),
                        Collectors.mapping(RangoDTO::de, Collectors.toList())));
        return ligaRepository.findAllByOrderByOrdenAsc().stream()
                .map(l -> LigaDTO.de(l, rangos.getOrDefault(l.getId(), List.of())))
                .toList();
    }

    /** Rango que corresponde a unos puntos de temporada. */
    @Transactional(readOnly = true)
    public Rango rangoPara(int puntos) {
        return rangoRepository.findFirstByPuntosMinimoLessThanEqualOrderByPuntosMinimoDesc(Math.max(puntos, 0))
                .orElseThrow(() -> new IllegalStateException("No hay rangos configurados."));
    }

    private static String nombreActividad(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
