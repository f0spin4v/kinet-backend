package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Perfil del usuario autenticado: ver, editar, cambiar contraseña y desactivar la cuenta. */
class PerfilIntegracionTest extends PruebaIntegracion {

    @Test
    void elJugadorEditaSuPerfil() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", "Ana María");
        datos.put("apellidos", "Gómez");
        datos.put("gamertag", "ana_pro");
        datos.put("avatarUrl", "https://img.kinet.app/ana.png");
        datos.put("fechaNacimiento", "2001-04-15");
        put("/api/usuarios/me", datos, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombres").value("Ana María"))
                .andExpect(jsonPath("$.respuesta.gamertag").value("ana_pro"))
                .andExpect(jsonPath("$.respuesta.fechaNacimiento").value("2001-04-15"));

        registrar("leo@kinet.com", "leo_run");
        datos.put("gamertag", "leo_run");
        put("/api/usuarios/me", datos, token).andExpect(status().isConflict());
    }

    @Test
    void cambiarLaContrasenaPideLaActual() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        put("/api/usuarios/me/contrasena", Map.of("contrasenaActual", "Mala1234", "nuevaContrasena", "Nueva2026"), token)
                .andExpect(status().isBadRequest());
        put("/api/usuarios/me/contrasena", Map.of("contrasenaActual", "Segura123", "nuevaContrasena", "Nueva2026"), token)
                .andExpect(status().isOk());
        iniciarSesion("ana@kinet.com", "Nueva2026");
    }

    @Test
    void desactivarLaCuentaImpideIniciarSesion() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        delete("/api/usuarios/me", token).andExpect(status().isOk());
        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden());
    }
}
