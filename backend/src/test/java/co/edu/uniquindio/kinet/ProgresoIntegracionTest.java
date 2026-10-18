package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dashboard en una sola consulta y bandeja de notificaciones. */
class ProgresoIntegracionTest extends PruebaIntegracion {

    @Test
    void elDashboardReuneTodoElProgreso() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/progreso", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.gamertag").value("leo_run"))
                .andExpect(jsonPath("$.respuesta.xpTotal").value(0))
                .andExpect(jsonPath("$.respuesta.nivel").value(1))
                .andExpect(jsonPath("$.respuesta.xpSiguienteNivel").value(100))
                .andExpect(jsonPath("$.respuesta.temporada").value("Temporada de prueba"))
                .andExpect(jsonPath("$.respuesta.rango").doesNotExist());

        evaluar(token, "INTERMEDIO");
        post("/api/retos/" + idReto("Misión diaria") + "/participaciones", null, token);
        post("/api/matchmaking/solicitudes", null, token);
        entrenar(token, "FUERZA", 50, "MEDIA", LocalDateTime.now(), null); // empieza después de unirse al reto: 150 + 20 del logro + 20 del reto diario

        get("/api/progreso", token)
                .andExpect(jsonPath("$.respuesta.xpTotal").value(190))
                .andExpect(jsonPath("$.respuesta.nivel").value(2))
                .andExpect(jsonPath("$.respuesta.xpInicioNivel").value(100))
                .andExpect(jsonPath("$.respuesta.xpSiguienteNivel").value(250))
                .andExpect(jsonPath("$.respuesta.porcentajeNivel").value(60))
                .andExpect(jsonPath("$.respuesta.entrenamientosSemana").value(1))
                .andExpect(jsonPath("$.respuesta.minutosSemana").value(50))
                .andExpect(jsonPath("$.respuesta.retosCompletados").value(1))
                .andExpect(jsonPath("$.respuesta.logros").value(1))
                .andExpect(jsonPath("$.respuesta.nivelHabilidad").value("INTERMEDIO"))
                .andExpect(jsonPath("$.respuesta.temporada").value("Temporada de prueba"))
                .andExpect(jsonPath("$.respuesta.rango").value("BRONCE III"))
                .andExpect(jsonPath("$.respuesta.puntosTemporada").value(0));
    }

    @Test
    void laBandejaSeMarcaComoLeida() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        entrenar(token, "FUERZA", 50, "MEDIA", haceHoras(2), null); // validado, nivel, logro
        get("/api/notificaciones", token)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(3))
                .andExpect(jsonPath("$.respuesta.contenido[0].estadoEnvio").value("ENVIADA"));
        long id = notificacionRepository.findAll().get(0).getId();
        patch("/api/notificaciones/" + id + "/leida", null, token)
                .andExpect(jsonPath("$.respuesta.leida").value(true))
                .andExpect(jsonPath("$.respuesta.fechaLectura").isNotEmpty());
        get("/api/progreso", token).andExpect(jsonPath("$.respuesta.notificacionesSinLeer").value(2));
        patch("/api/notificaciones/leidas", null, token).andExpect(status().isOk());
        get("/api/progreso", token).andExpect(jsonPath("$.respuesta.notificacionesSinLeer").value(0));
        patch("/api/notificaciones/" + id + "/leida", null, registrar("ana@kinet.com", "ana_gg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void lasActividadesMuestranSuXpVigente() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/catalogos/actividades", token)
                .andExpect(jsonPath("$.respuesta[?(@.nombre=='RUNNING')].reglasVigentes.length()").value(3));
        get("/api/catalogos/ligas", token).andExpect(jsonPath("$.respuesta[0].rangos.length()").value(3));
        get("/api/notificaciones", token).andExpect(jsonPath("$.respuesta.contenido.length()").value(0));
    }
}
