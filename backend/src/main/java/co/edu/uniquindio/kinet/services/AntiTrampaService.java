package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.model.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;

/** Proceso 2: módulo de validación de entrenamientos (anti-cheat, RNF-24). */
public interface AntiTrampaService {

    /** Devuelve el motivo de rechazo, o vacío si el entrenamiento es válido. */
    Optional<String> validar(Usuario usuario, LocalDateTime inicio, int duracionMinutos);
}
