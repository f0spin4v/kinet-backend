package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 1: registro, inicio de sesión y protección de rutas (RNF-12, RNF-21, RNF-22, RNF-26). */
class AuthIntegracionTest extends PruebaIntegracion {

    @Test
    void registroCreaJugadorConContrasenaCifradaYCorreoEnMinusculas() throws Exception {
        post("/api/auth/registro", datosRegistro("Ana@Kinet.com", "ana_gg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.error").value(false))
                .andExpect(jsonPath("$.respuesta.token").isNotEmpty())
                .andExpect(jsonPath("$.respuesta.usuario.correo").value("ana@kinet.com"))
                .andExpect(jsonPath("$.respuesta.usuario.rol").value("JUGADOR"))
                .andExpect(jsonPath("$.respuesta.usuario.nickname").value("ana_gg"))
                .andExpect(jsonPath("$.respuesta.usuario.nivelInicial").doesNotExist());

        Usuario usuario = usuarioRepository.findByCorreo("ana@kinet.com").orElseThrow();
        assertThat(usuario.getContrasenaHash()).startsWith("$2").doesNotContain("Segura123");
        assertThat(usuario.getAceptaDatos()).isTrue();
        assertThat(progresoRepository.findByUsuarioId(usuario.getId())).isPresent();
    }

    @Test
    void registroRechazaCorreoONicknameRepetidos() throws Exception {
        registrar("ana@kinet.com", "ana_gg");

        post("/api/auth/registro", datosRegistro("ANA@kinet.com", "otra_ana"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.respuesta").value(
                        "Ya existe una cuenta con ese correo. Inicia sesión o recupera tu contraseña."));
        post("/api/auth/registro", datosRegistro("otra@kinet.com", "ana_gg"))
                .andExpect(status().isConflict());
    }

    @Test
    void registroExplicaCadaCampoInvalido() throws Exception {
        Map<String, Object> datos = datosRegistro("no-es-correo", "x");
        datos.put("contrasena", "corta");
        datos.put("aceptaDatos", false);

        post("/api/auth/registro", datos)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.respuesta[*].campo", hasItems("correo", "contrasena", "nickname", "aceptaDatos")));
    }

    @Test
    void loginDevuelveTokenYRechazaCredencialesConElMismoMensaje() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        String token = iniciarSesion("ANA@kinet.com", "Segura123");
        assertThat(token).isNotBlank();

        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Equivocada1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.respuesta").value("Correo o contraseña incorrectos."));
        post("/api/auth/login", Map.of("correo", "nadie@kinet.com", "contrasena", "Segura123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.respuesta").value("Correo o contraseña incorrectos."));
    }

    @Test
    void unaCuentaBloqueadaNoPuedeEntrar() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        Usuario usuario = usuarioRepository.findByCorreo("ana@kinet.com").orElseThrow();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuarioRepository.save(usuario);

        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Segura123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasRutasProtegidasExigenUnTokenValido() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(true));
        get("/api/usuarios/me", token + "x").andExpect(status().isUnauthorized());
        get("/api/usuarios/me", "no.es.token").andExpect(status().isUnauthorized());
        get("/api/usuarios/me", token).andExpect(status().isOk());
    }

    @Test
    void erroresDeFormatoYRutasInexistentesResponden4xxConMensaje() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true));
        get("/api/no-existe", token).andExpect(status().isNotFound());
        get("/api/entrenamientos/abc", token).andExpect(status().isBadRequest());
    }
}
