package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.progreso.ProgresoDTO;
import co.edu.uniquindio.kinet.model.ProgresoJugador;
import co.edu.uniquindio.kinet.model.Usuario;

import java.time.LocalDate;

/** Proceso 2: XP, nivel y racha del jugador. */
public interface ProgresoService {

    ProgresoJugador obtenerOCrear(Usuario usuario);

    void actualizarRacha(ProgresoJugador progreso, LocalDate diaEntrenamiento);

    /** Suma XP, recalcula el nivel y avisa si subió. Devuelve true si subió de nivel. */
    boolean sumarXp(ProgresoJugador progreso, int xp);

    int rachaVigente(ProgresoJugador progreso);

    ProgresoDTO resumen(String correo);
}
