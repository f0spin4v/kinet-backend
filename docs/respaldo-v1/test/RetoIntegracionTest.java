package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.ParticipacionReto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 2: retos por nivel, participación, avance y recompensa. */
class RetoIntegracionTest extends PruebaIntegracion {

    private static final List<String> INICIACION = List.of("A", "A", "A", "A", "A");

    private long idReto(String titulo) {
        return retoRepository.findAll().stream().filter(r -> r.getTitulo().equals(titulo)).findFirst()
                .orElseThrow().getId();
    }

    /** Simula que el jugador se unió hace unas horas. */
    private void unirseHaceHoras(long horas) {
        participacionRepository.findAll().forEach(p -> {
            p.setFechaInicio(p.getFechaInicio().minusHours(horas));
            participacionRepository.save(p);
        });
    }

    @Test
    void sinEvaluacionNoHayRetos() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/retos", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Primero haz tu evaluación de nivel para ver tus retos."));
    }

    @Test
    void losRetosSeFiltranPorNivel() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        evaluar(token, INICIACION);

        get("/api/retos", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta[*].titulo", hasItem("Arranque")))
                .andExpect(jsonPath("$.respuesta[*].titulo", hasItem("Primer paso")))
                .andExpect(jsonPath("$.respuesta[*].titulo", not(hasItem("Modo élite"))));
    }

    @Test
    void unirseUnaSolaVezYSoloASuNivel() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        evaluar(token, INICIACION);

        post("/api/retos/" + idReto("Arranque") + "/participaciones", null, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.respuesta.porcentaje").value(0));
        post("/api/retos/" + idReto("Arranque") + "/participaciones", null, token)
                .andExpect(status().isConflict());
        post("/api/retos/" + idReto("Modo élite") + "/participaciones", null, token)
                .andExpect(status().isBadRequest());
        post("/api/retos/99999/participaciones", null, token).andExpect(status().isNotFound());
    }

    @Test
    void entrenarAvanzaElRetoYAlCompletarloDaXpExtra() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        evaluar(token, INICIACION);
        post("/api/retos/" + idReto("Arranque") + "/participaciones", null, token);     // meta: 60 minutos
        post("/api/retos/" + idReto("Primer paso") + "/participaciones", null, token);  // meta: 1 sesión
        unirseHaceHoras(5);

        // 40 min de gimnasio baja = 80 XP; completa "Primer paso" (+30 XP)
        entrenar(token, "GIMNASIO", 40, "BAJA", haceHoras(4))
                .andExpect(jsonPath("$.respuesta.retosCompletados[0]").value("Primer paso"))
                .andExpect(jsonPath("$.respuesta.xpGanado").value(110));

        get("/api/retos/" + idReto("Arranque"), token)
                .andExpect(jsonPath("$.respuesta.participacion.progreso").value(40))
                .andExpect(jsonPath("$.respuesta.participacion.porcentaje").value(66));

        // 30 min más completan "Arranque" (+50 XP)
        entrenar(token, "GIMNASIO", 30, "BAJA", haceHoras(2))
                .andExpect(jsonPath("$.respuesta.retosCompletados[0]").value("Arranque"))
                .andExpect(jsonPath("$.respuesta.xpTotal").value(80 + 30 + 60 + 50));

        get("/api/retos/mios", token)
                .andExpect(jsonPath("$.respuesta.length()").value(2))
                .andExpect(jsonPath("$.respuesta[0].estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.respuesta[1].estado").value("COMPLETADO"));
        get("/api/progreso", token).andExpect(jsonPath("$.respuesta.retosCompletados").value(2));
    }

    @Test
    void unRetoDeUnaActividadSoloCuentaEsaActividad() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        evaluar(token, INICIACION);
        post("/api/retos/" + idReto("Corredor") + "/participaciones", null, token);
        unirseHaceHoras(5);

        entrenar(token, "GIMNASIO", 30, "BAJA", haceHoras(4));
        entrenar(token, "RUNNING", 30, "BAJA", haceHoras(2));

        get("/api/retos/" + idReto("Corredor"), token)
                .andExpect(jsonPath("$.respuesta.participacion.progreso").value(30));
    }

    @Test
    void unRetoSinTerminarEnSuPlazoQuedaVencido() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        evaluar(token, INICIACION);
        post("/api/retos/" + idReto("Arranque") + "/participaciones", null, token);
        for (ParticipacionReto p : participacionRepository.findAll()) {
            p.setFechaLimite(LocalDateTime.now().minusMinutes(1));
            participacionRepository.save(p);
        }

        get("/api/retos/mios", token).andExpect(jsonPath("$.respuesta[0].estado").value("VENCIDO"));
        // vencido, puede volver a intentarlo
        post("/api/retos/" + idReto("Arranque") + "/participaciones", null, token).andExpect(status().isCreated());
    }
}
