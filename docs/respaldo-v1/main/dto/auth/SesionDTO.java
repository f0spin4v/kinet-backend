package co.edu.uniquindio.kinet.dto.auth;

import co.edu.uniquindio.kinet.dto.usuario.UsuarioDTO;

/** Lo que recibe el frontend al registrarse o iniciar sesión. */
public record SesionDTO(String token, UsuarioDTO usuario) {
}
