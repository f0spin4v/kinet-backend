package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.entrenamiento.EditarActividadDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.TipoActividadDTO;

import java.util.List;

/** Proceso 2: catálogo de tipos de actividad y su XP por minuto. */
public interface ActividadService {

    List<TipoActividadDTO> listar();

    TipoActividadDTO editarXp(String codigo, EditarActividadDTO editarDTO);
}
