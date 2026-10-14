package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.reto.CrearRetoDTO;
import co.edu.uniquindio.kinet.dto.reto.ParticipacionDTO;
import co.edu.uniquindio.kinet.dto.reto.RetoDTO;
import co.edu.uniquindio.kinet.model.Entrenamiento;
import co.edu.uniquindio.kinet.model.ParticipacionReto;
import co.edu.uniquindio.kinet.model.Usuario;

import java.util.List;

/** Proceso 2: retos y misiones. */
public interface RetoService {

    List<RetoDTO> listarDisponibles(String correo);

    RetoDTO detalle(String correo, Long idReto);

    ParticipacionDTO unirse(String correo, Long idReto);

    List<ParticipacionDTO> misRetos(String correo);

    /** Suma un entrenamiento validado a los retos en curso. Devuelve los que quedaron completados. */
    List<ParticipacionReto> avanzarRetos(Usuario usuario, Entrenamiento entrenamiento);

    RetoDTO crear(CrearRetoDTO crearDTO);

    List<RetoDTO> listarTodos();

    RetoDTO cambiarEstado(Long idReto, boolean activo);
}
