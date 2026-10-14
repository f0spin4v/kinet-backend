package co.edu.uniquindio.kinet.services;

import co.edu.uniquindio.kinet.dto.auth.LoginDTO;
import co.edu.uniquindio.kinet.dto.auth.RecuperarContrasenaDTO;
import co.edu.uniquindio.kinet.dto.auth.RegistroDTO;
import co.edu.uniquindio.kinet.dto.auth.RestablecerContrasenaDTO;
import co.edu.uniquindio.kinet.dto.auth.SesionDTO;

/** Proceso 1: registro, inicio de sesión y recuperación de contraseña. */
public interface AuthService {

    SesionDTO registrar(RegistroDTO registroDTO);

    SesionDTO iniciarSesion(LoginDTO loginDTO);

    /** Envía el enlace de recuperación si el correo existe. No revela si la cuenta existe o no. */
    void solicitarRecuperacion(RecuperarContrasenaDTO recuperarDTO);

    void restablecerContrasena(RestablecerContrasenaDTO restablecerDTO);
}
