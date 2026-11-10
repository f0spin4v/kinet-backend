package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Correcciones de la auditoría del 03/11/2026: seguridad, Ley 1581, bitácora, monitoreo y evidencias. */
class AuditoriaIntegracionTest extends PruebaIntegracion {

    // ---------- 6.7.2: el token se revalida contra la base en cada petición ----------

    @Test
    void unaCuentaBloqueadaPierdeElAccesoDeInmediatoAunqueSuTokenSigaVigente() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/usuarios/me", token).andExpect(status().isOk());

        Usuario usuario = usuarioRepository.findByCorreo("leo@kinet.com").orElseThrow();
        usuario.setEstado(EstadoUsuario.BLOQUEADO);
        usuarioRepository.save(usuario);

        get("/api/usuarios/me", token).andExpect(status().isUnauthorized());
    }

    @Test
    void unCambioDeRolAplicaEnLaSiguientePeticionSinVolverAIniciarSesion() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        get("/api/admin/retos", admin).andExpect(status().isOk());

        Usuario usuario = usuarioRepository.findByCorreo("admin@kinet.com").orElseThrow();
        usuario.setRol(rolRepository.findByNombre(Rol.JUGADOR).orElseThrow());
        usuarioRepository.save(usuario);

        // el token todavía dice ADMINISTRADOR, pero manda el rol que está en la base
        get("/api/admin/retos", admin).andExpect(status().isForbidden());
    }

    // ---------- 6.7.5: límite de intentos de inicio de sesión ----------

    @Test
    void despuesDeCincoIntentosFallidosElCorreoQuedaFrenado() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        for (int i = 0; i < 5; i++) {
            post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Mala1234"), null)
                    .andExpect(status().isUnauthorized());
        }
        // ni siquiera con la contraseña correcta: hay que esperar
        post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.respuesta", containsString("Demasiados intentos")));
        // otro correo no se ve afectado
        registrar("leo@kinet.com", "leo_run");
        iniciarSesion("leo@kinet.com", "Segura123");
    }

    @Test
    void unInicioDeSesionCorrectoReiniciaElContador() throws Exception {
        registrar("ana@kinet.com", "ana_gg");
        for (int i = 0; i < 4; i++) {
            post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Mala1234"), null);
        }
        iniciarSesion("ana@kinet.com", "Segura123");
        for (int i = 0; i < 4; i++) {
            post("/api/auth/login", Map.of("correo", "ana@kinet.com", "contrasena", "Mala1234"), null)
                    .andExpect(status().isUnauthorized());
        }
        iniciarSesion("ana@kinet.com", "Segura123");
    }

    // ---------- Ley 1581: edad mínima y consentimiento del titular ----------

    @Test
    void unMenorDe14NoPuedeRegistrarseYUnoDe17NecesitaASuRepresentante() throws Exception {
        Map<String, Object> datos = datosRegistro("menor@kinet.com", "menor_k");
        datos.put("fechaNacimiento", LocalDate.now().minusYears(13).toString());
        post("/api/auth/registro", datos, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("mayores de 14")));
        // RN-33: de 14 a 17 años hace falta el representante legal
        datos.put("fechaNacimiento", LocalDate.now().minusYears(17).toString());
        post("/api/auth/registro", datos, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("representante legal")));
        assertThat(usuarioRepository.existsByCorreo("menor@kinet.com")).isFalse();

        Map<String, Object> sinFecha = datosRegistro("sinfecha@kinet.com", "sin_fecha");
        sinFecha.remove("fechaNacimiento");
        post("/api/auth/registro", sinFecha, null).andExpect(status().isBadRequest());
    }

    @Test
    void elPerfilNoAceptaUnaFechaDeNacimientoDeMenor() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        put("/api/usuarios/me", Map.of("nombres", "Leo", "apellidos", "Run", "gamertag", "leo_run",
                "fechaNacimiento", LocalDate.now().minusYears(10).toString()), token)
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCuentaQueCreaElAdministradorQuedaSinConsentimientoHastaQueElTitularLoDa() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        post("/api/admin/usuarios", Map.of("nombres", "Eva", "apellidos", "Coach", "correo", "eva@kinet.com",
                "contrasena", "Temporal123", "gamertag", "eva_coach", "rol", "ENTRENADOR"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.rol").value("ENTRENADOR"))
                .andExpect(jsonPath("$.respuesta.aceptaPolitica").value(false));
        assertThat(usuarioRepository.findByCorreo("eva@kinet.com").orElseThrow().getAceptaPoliticaDatos()).isFalse();

        // el titular entra y acepta él mismo
        String eva = iniciarSesion("eva@kinet.com", "Temporal123");
        put("/api/usuarios/me/politica", null, eva)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.aceptaPolitica").value(true));
        Usuario usuario = usuarioRepository.findByCorreo("eva@kinet.com").orElseThrow();
        assertThat(usuario.getAceptaPoliticaDatos()).isTrue();
        assertThat(usuario.getFechaAceptaPolitica()).isEqualTo(LocalDate.now());
    }

    @Test
    void soloElAdministradorCreaCuentas() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        post("/api/admin/usuarios", Map.of("nombres", "X", "apellidos", "Y", "correo", "x@kinet.com",
                "contrasena", "Temporal123", "gamertag", "x_y", "rol", "ADMINISTRADOR"), jugador)
                .andExpect(status().isForbidden());
    }

    // ---------- 6.7.9: bitácora de acciones administrativas ----------

    @Test
    void lasAccionesDelAdministradorQuedanEnLaBitacora() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        registrar("leo@kinet.com", "leo_run");
        Long leo = idDe("leo@kinet.com");

        post("/api/soporte/ajustes-xp", Map.of("idUsuario", leo, "cantidad", 50, "motivo", "Corrección de prueba"), admin)
                .andExpect(status().isCreated());
        patch("/api/soporte/usuarios/" + leo + "/estado", Map.of("estado", "BLOQUEADO"), admin).andExpect(status().isOk());
        patch("/api/admin/usuarios/" + leo + "/rol", Map.of("rol", "ENTRENADOR"), admin).andExpect(status().isOk());

        get("/api/admin/bitacora", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.totalElementos").value(3))
                // lo más reciente primero
                .andExpect(jsonPath("$.respuesta.contenido[0].accion").value("CAMBIO_ROL"))
                .andExpect(jsonPath("$.respuesta.contenido[0].valorAnterior").value("JUGADOR"))
                .andExpect(jsonPath("$.respuesta.contenido[0].valorNuevo").value("ENTRENADOR"))
                .andExpect(jsonPath("$.respuesta.contenido[1].accion").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$.respuesta.contenido[1].valorAnterior").value("ACTIVO"))
                .andExpect(jsonPath("$.respuesta.contenido[1].valorNuevo").value("BLOQUEADO"))
                .andExpect(jsonPath("$.respuesta.contenido[1].correoActor").value("admin@kinet.com"))
                .andExpect(jsonPath("$.respuesta.contenido[1].correoAfectado").value("leo@kinet.com"))
                .andExpect(jsonPath("$.respuesta.contenido[2].accion").value("AJUSTE_XP"))
                .andExpect(jsonPath("$.respuesta.contenido[2].valorNuevo", containsString("+50 XP")));
    }

    @Test
    void laBitacoraEsSoloParaElAdministrador() throws Exception {
        String entrenador = cuentaConRol("coach@kinet.com", "coach_k", "ENTRENADOR");
        get("/api/admin/bitacora", entrenador).andExpect(status().isForbidden());
    }

    // ---------- 6.10.6: monitoreo ----------

    @Test
    void elEstadoDeSaludEsPublicoYNoMuestraDetalles() throws Exception {
        get("/api/sistema/health", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        // el resto de Actuator no está expuesto
        get("/api/sistema/env", null).andExpect(status().isUnauthorized());
    }

    // ---------- NC-03: datos de la evidencia sin la imagen ----------

    @Test
    void elDetalleRecibeLosDatosDeLaEvidenciaAunqueSeaUnEnlace() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 40, "MEDIA", haceHoras(2), null)).at("/entrenamiento/id").asLong();

        get("/api/entrenamientos/" + id + "/evidencia/datos", token).andExpect(status().isNotFound());

        subirEnlace(token, id, "https://www.strava.com/activities/7770001").andExpect(status().isCreated());
        get("/api/entrenamientos/" + id + "/evidencia/datos", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.tipo").value("ENLACE"))
                .andExpect(jsonPath("$.respuesta.url").value("https://www.strava.com/activities/7770001"));

        // la evidencia de otro jugador no se ve
        String ana = registrar("ana@kinet.com", "ana_gg");
        get("/api/entrenamientos/" + id + "/evidencia/datos", ana).andExpect(status().isNotFound());
    }
}
