package co.edu.uniquindio.kinet.dto.usuario;

import java.time.LocalDate;

/**
 * Datos del usuario que se devuelven al frontend. Nunca incluye la contraseña.
 * nivelInicial es null mientras el jugador no haya hecho la evaluación de nivel.
 */
public record UsuarioDTO(
        Long id,
        String nombre,
        String correo,
        String rol,
        String nickname,
        String avatarUrl,
        LocalDate fechaNacimiento,
        String ciudad,
        String deporteFavorito,
        String nivelInicial
) {
}
