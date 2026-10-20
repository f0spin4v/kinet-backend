package co.edu.uniquindio.kinet;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RNF-22: solo un JWT firmado por el servidor y vigente abre la API; lo demás responde 401. */
class SeguridadTokenTest extends PruebaIntegracion {

    @Value("${kinet.jwt.secret}")
    private String secreto;

    @Test
    void sinTokenOConUnEncabezadoMalFormadoRespondeNoAutorizado() throws Exception {
        registrar("leo@kinet.com", "leo_run");

        get("/api/usuarios/me", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.respuesta", containsString("Inicia sesión de nuevo")));
        // sin el prefijo "Bearer " el token se ignora
        String token = iniciarSesion("leo@kinet.com", "Segura123");
        mvc.perform(MockMvcRequestBuilders.get("/api/usuarios/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        get("/api/usuarios/me", "no.es.un-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenGeneradoConLaClaveDelServidorSiFunciona() throws Exception {
        // control: el generador de esta clase produce tokens válidos, así las demás pruebas miden lo que dicen
        registrar("leo@kinet.com", "leo_run");
        get("/api/usuarios/me", token(secreto, "leo@kinet.com", "JUGADOR", Duration.ofMinutes(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.correo").value("leo@kinet.com"));
    }

    @Test
    void unTokenVencidoNoSirve() throws Exception {
        registrar("leo@kinet.com", "leo_run");
        String vencido = token(secreto, "leo@kinet.com", "JUGADOR", Duration.ofMinutes(-5));

        get("/api/usuarios/me", vencido).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtraClaveNoSirve() throws Exception {
        cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String falsificado = token("otra-clave-cualquiera-de-mas-de-32-caracteres", "admin@kinet.com",
                "ADMINISTRADOR", Duration.ofMinutes(10));

        get("/api/admin/retos", falsificado).andExpect(status().isUnauthorized());
        get("/api/usuarios/me", falsificado).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarElRolDentroDelTokenRompeLaFirma() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        get("/api/admin/retos", jugador).andExpect(status().isForbidden());

        // se reescribe el payload pidiendo rol de administrador y se deja la firma original
        String[] partes = jugador.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"JUGADOR\"", "\"ADMINISTRADOR\"");
        String alterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];

        get("/api/admin/retos", alterado).andExpect(status().isUnauthorized());
    }

    @Test
    void lasRutasDeCuentaSonPublicasYElRestoPideSesion() throws Exception {
        post("/api/auth/recuperar-contrasena", Map.of("correo", "nadie@kinet.com"), null).andExpect(status().isOk());
        post("/api/auth/login", Map.of("correo", "nadie@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isUnauthorized());
        registrar("leo@kinet.com", "leo_run");

        for (String ruta : new String[]{"/api/catalogos/actividades", "/api/progreso", "/api/notificaciones",
                "/api/temporadas/activa/ranking", "/api/entrenamientos", "/api/retos"}) {
            get(ruta, null).andExpect(status().isUnauthorized());
        }
    }

    private static String token(String clave, String correo, String rol, Duration vigencia) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora.minus(Duration.ofHours(1))))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(Keys.hmacShaKeyFor(clave.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
