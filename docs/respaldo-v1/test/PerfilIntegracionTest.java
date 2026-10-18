package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.services.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 1: gestión de perfil (UI-002), cambio y recuperación de contraseña (RNF-08). */
class PerfilIntegracionTest extends PruebaIntegracion {

    @MockitoBean
    EmailService emailService;

    private Map<String, Object> perfil(String nickname) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Ana Gamer");
        datos.put("nickname", nickname);
        datos.put("avatarUrl", "https://kinet.app/avatar.png");
        datos.put("fechaNacimiento", "2000-05-20");
        datos.put("ciudad", "Armenia");
        datos.put("deporteFavorito", "Running");
        return datos;
    }

    @Test
    void elJugadorVeYEditaSuPerfil() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");

        put("/api/usuarios/me", perfil("ana_pro"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nickname").value("ana_pro"))
                .andExpect(jsonPath("$.respuesta.ciudad").value("Armenia"))
                .andExpect(jsonPath("$.respuesta.fechaNacimiento").value("2000-05-20"));

        get("/api/usuarios/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombre").value("Ana Gamer"))
                .andExpect(jsonPath("$.respuesta.deporteFavorito").value("Running"));
    }

    @Test
    void noSePuedeUsarElNicknameDeOtroNiDatosInvalidos() throws Exception {
        registrar("leo@kinet.com", "leo_run");
        String token = registrar("ana@kinet.com", "ana_gg");

        put("/api/usuarios/me", perfil("leo_run"), token).andExpect(status().isConflict());
        put("/api/usuarios/me", perfil("ana_gg"), token).andExpect(status().isOk()); // el suyo sí

        Map<String, Object> malo = perfil("ana_gg");
        malo.put("avatarUrl", "ftp://archivo");
        malo.put("fechaNacimiento", "2999-01-01");
        put("/api/usuarios/me", malo, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta.length()").value(2));
    }

    @Test
    void cambiarContrasenaExigeLaActual() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");

        put("/api/usuarios/me/contrasena", Map.of("contrasenaActual", "Mala1234", "nuevaContrasena", "Nueva2026"), token)
                .andExpect(status().isBadRequest());
        put("/api/usuarios/me/contrasena", Map.of("contrasenaActual", "Segura123", "nuevaContrasena", "Nueva2026"), token)
                .andExpect(status().isOk());
        iniciarSesion("ana@kinet.com", "Nueva2026");
    }

    @Test
    void recuperarContrasenaConElEnlaceDeUnSoloUso() throws Exception {
        registrar("ana@kinet.com", "ana_gg");

        post("/api/auth/recuperar-contrasena", Map.of("correo", "ana@kinet.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(false));
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRecuperacion(eq("ana@kinet.com"), anyString(), token.capture());

        Map<String, Object> restablecer = Map.of("token", token.getValue(), "nuevaContrasena", "Recuperada1");
        post("/api/auth/restablecer-contrasena", restablecer).andExpect(status().isOk());
        iniciarSesion("ana@kinet.com", "Recuperada1");

        // el mismo enlace no sirve dos veces
        post("/api/auth/restablecer-contrasena", restablecer).andExpect(status().isBadRequest());
    }

    @Test
    void recuperarNoRevelaSiElCorreoExisteYRechazaCodigosFalsos() throws Exception {
        post("/api/auth/recuperar-contrasena", Map.of("correo", "nadie@kinet.com"))
                .andExpect(status().isOk());
        verify(emailService, never()).enviarRecuperacion(anyString(), anyString(), anyString());

        post("/api/auth/restablecer-contrasena", Map.of("token", "inventado", "nuevaContrasena", "Recuperada1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value(
                        "El enlace de recuperación no es válido o ya venció. Solicita uno nuevo."));
    }
}
