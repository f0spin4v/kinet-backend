package co.edu.uniquindio.kinet.services.impl;

import co.edu.uniquindio.kinet.dto.entrenamiento.EditarActividadDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.TipoActividadDTO;
import co.edu.uniquindio.kinet.exceptions.NoEncontradoException;
import co.edu.uniquindio.kinet.mappers.EntrenamientoMapper;
import co.edu.uniquindio.kinet.model.TipoActividad;
import co.edu.uniquindio.kinet.repositories.TipoActividadRepository;
import co.edu.uniquindio.kinet.services.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActividadServiceImpl implements ActividadService {

    private final TipoActividadRepository actividadRepository;
    private final EntrenamientoMapper entrenamientoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TipoActividadDTO> listar() {
        return actividadRepository.findAllByOrderByNombreAsc().stream()
                .map(entrenamientoMapper::toTipoActividadDTO)
                .toList();
    }

    @Override
    @Transactional
    public TipoActividadDTO editarXp(String codigo, EditarActividadDTO editarDTO) {
        TipoActividad actividad = actividadRepository.findByCodigo(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontradoException("La actividad no existe."));
        actividad.setXpPorMinuto(editarDTO.xpPorMinuto());
        return entrenamientoMapper.toTipoActividadDTO(actividadRepository.save(actividad));
    }
}
