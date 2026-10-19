package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.TokenRecuperacion;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 1: registro, inicio de sesión, estados de cuenta y recuperación de contraseña. */
class AuthIntegracionTest extends PruebaIntegracion {

    @Test
    void elRegistroCreaUnJugadorConPerfilEnCero() throws Exception {
        post("/api/auth/registro", datosRegistro("Ana@Kinet.com", "ana_gg"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.token").isNotEmpty())
                .andExpect(jsonPath("$.respuesta.usuario.correo").value("ana@kinet.com"))
                .andExpect(jsonPath("$.respuesta.usuario.rol").value("JUGADOR"))
                .andExpect(jsonPath("$.respuesta.usuario.gamertag").value("ana_gg"))
                .andExpect(jsonPath("$.respuesta.usuario.nivelHabilidad").doesNotExist());
        Usuario usuario = usuarioRepository.findByCorreo("ana@kinet.com").orElseThrow();
        assertThat(usuario.getPasswordHash()).startsWith("$2a$").doesNotContain("Segura123");
        assertThat(perfilRepository.findById(usuario.getId()).orElseThrow().getXpTotal()).isZero();
    }

    @Test
    void elCorreoYElGamertagSonUnicos() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        post("/api/auth/registro", datosRegistro("ANA@kinet.com", "otra"), null).andExpect(status().isConflict());
        post("/api/auth/registro", datosRegistro("otra@kinet.com", "ana_gg"), null).andExpect(status().isConflict());
    }

    @Test
    void elRegistroValidaLosCampos() throws Exception {
        Map<String, Object> datos = datosRegistro("no-es-correo", "a");
        datos.put("contrasena", "corta");
        datos.put("aceptaPolitica", false);
        post("/api/auth/registro", datos, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.respuesta.length()").value(5));
    }

    @Test
    void elLoginRechazaCredencialesYCuentasNoActivas() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Mala1234"), null)
                .andExpect(status().isUnauthorized());

        Usuario usuario = usuarioRepository.findByCorreo("ana@kinet.com").orElseThrow();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuarioRepository.save(usuario);
        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.respuesta").value("Tu cuenta está bloqueada. Contacta a soporte."));

        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenNoHayAcceso() throws Exception {
        get("/api/usuarios/me", null).andExpect(status().isUnauthorized());
        get("/api/usuarios/me", "token-falso").andExpect(status().isUnauthorized());
    }

    @Test
    void laRecuperacionNoRevelaSiElCorreoExiste() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        post("/api/auth/recuperar-contrasena", Map.of("correo", "ana@kinet.com"), null).andExpect(status().isOk());
        post("/api/auth/recuperar-contrasena", Map.of("correo", "nadie@kinet.com"), null).andExpect(status().isOk());
        assertThat(tokenRepository.findAll()).hasSize(1);
        // un enlace nuevo invalida el anterior
        post("/api/auth/recuperar-contrasena", Map.of("correo", "ana@kinet.com"), null).andExpect(status().isOk());
        assertThat(tokenRepository.findAll()).filteredOn(t -> t.getFechaUso() == null).hasSize(1);
    }

    @Test
    void elEnlaceDeRecuperacionSirveUnaSolaVez() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        Usuario usuario = usuarioRepository.findByCorreo("ana@kinet.com").orElseThrow();
        tokenRepository.save(TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(sha256("token-de-prueba"))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30))
                .build());

        Map<String, Object> datos = Map.of("token", "token-de-prueba", "nuevaContrasena", "Nueva2026");
        post("/api/auth/restablecer-contrasena", datos, null).andExpect(status().isOk());
        iniciarSesion("ana@kinet.com", "Nueva2026");
        post("/api/auth/restablecer-contrasena", datos, null).andExpect(status().isBadRequest());
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }
}
