package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.entrenamiento.EntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.RegistrarEntrenamientoDTO;
import co.edu.uniquindio.kinet.dto.entrenamiento.ResultadoEntrenamientoDTO;

import java.util.List;

/** Proceso 2: registro de entrenamientos y su conversión en XP, racha y avance de retos. */
public interface EntrenamientoService {

    ResultadoEntrenamientoDTO registrar(String correo, RegistrarEntrenamientoDTO registrarDTO);

    List<EntrenamientoDTO> historial(String correo);

    EntrenamientoDTO obtener(String correo, Long idEntrenamiento);
}
