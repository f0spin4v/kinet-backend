package co.edu.uniquindio.kinet;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Retos: visibilidad por nivel, periodos, aportes automáticos y manuales, retos de clan y abandono. */
class RetoIntegracionTest extends PruebaIntegracion {

    @Test
    void cadaJugadorVeLosRetosDeTodosYLosDeSuNivel() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/retos", token)
                .andExpect(jsonPath("$.respuesta.contenido[*].nombre", hasItem("Primer paso")))
                .andExpect(jsonPath("$.respuesta.contenido[*].nombre", not(hasItem("Modo élite"))))
                .andExpect(jsonPath("$.respuesta.contenido[*].nombre", not(hasItem("Reto en borrador"))));
        post("/api/retos/" + idReto("Modo élite") + "/participaciones", null, token).andExpect(status().isBadRequest());

        evaluar(token, "AVANZADO");
        get("/api/retos", token).andExpect(jsonPath("$.respuesta.contenido[*].nombre", hasItem("Modo élite")));
    }

    @Test
    void unEntrenamientoAvanzaYCompletaLosRetos() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/retos/" + idReto("Primer paso") + "/participaciones", null, token).andExpect(status().isCreated());
        post("/api/retos/" + idReto("Misión diaria") + "/participaciones", null, token).andExpect(status().isCreated());

        // 20 min: completa "Primer paso" (1 sesión) pero no "Misión diaria" (30 min)
        entrenar(token, "CALISTENIA", 20, "MEDIA", LocalDateTime.now(), null)
                .andExpect(jsonPath("$.respuesta.retosCompletados[0]").value("Primer paso"))
                .andExpect(jsonPath("$.respuesta.retosCompletados.length()").value(1));
        get("/api/retos/mios", token)
                .andExpect(jsonPath("$.respuesta.contenido[?(@.reto=='Misión diaria')].progreso").value(20))
                .andExpect(jsonPath("$.respuesta.contenido[?(@.reto=='Misión diaria')].porcentaje").value(66))
                .andExpect(jsonPath("$.respuesta.contenido[?(@.reto=='Primer paso')].estado").value("COMPLETADO"));
        get("/api/progreso/movimientos", token).andExpect(jsonPath("$.respuesta.contenido[?(@.origen=='RETO')].cantidad").value(30));
    }

    @Test
    void unRetoSoloSeTomaUnaVezPorPeriodo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/retos/" + idReto("Misión diaria") + "/participaciones", null, token).andExpect(status().isCreated());
        post("/api/retos/" + idReto("Misión diaria") + "/participaciones", null, token).andExpect(status().isConflict());
    }

    @Test
    void losRetosDeRepeticionesSeAvanzanConAportesManuales() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(post("/api/retos/" + idReto("Cien flexiones") + "/participaciones", null, token)).at("/id").asLong();
        post("/api/retos/participaciones/" + id + "/aportes", Map.of("cantidad", 60, "nota", "3 series"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.progreso").value(60.0));
        post("/api/retos/participaciones/" + id + "/aportes", Map.of("cantidad", 40), token)
                .andExpect(jsonPath("$.respuesta.estado").value("COMPLETADO"));

        long otro = respuesta(post("/api/retos/" + idReto("Primer paso") + "/participaciones", null, token)).at("/id").asLong();
        post("/api/retos/participaciones/" + otro + "/aportes", Map.of("cantidad", 1), token)
                .andExpect(status().isBadRequest());
    }

    @Test
    void unRetoDeClanSeCompletaConLaSumaDelClan() throws Exception {
        String lider = registrar("leo@kinet.com", "leo_run");
        String miembro = registrar("ana@kinet.com", "ana_gg");
        post("/api/retos/" + idReto("Minutos en equipo") + "/participaciones", null, lider)
                .andExpect(status().isBadRequest());

        long clan = respuesta(post("/api/clanes", Map.of("nombre", "Los Rápidos", "capacidadMax", 10), lider)).at("/clan/id").asLong();
        post("/api/clanes/" + clan + "/miembros", null, miembro).andExpect(status().isCreated());
        post("/api/retos/" + idReto("Minutos en equipo") + "/participaciones", null, lider).andExpect(status().isCreated());
        post("/api/retos/" + idReto("Minutos en equipo") + "/participaciones", null, miembro).andExpect(status().isCreated());

        // la distancia es informativa: no cuenta para ningún reto
        entrenar(lider, "RUNNING", 40, "MEDIA", LocalDateTime.now(), 12.0)
                .andExpect(jsonPath("$.respuesta.retosCompletados.length()").value(0));
        JsonNode resultado = respuesta(entrenar(miembro, "CICLISMO", 30, "MEDIA", LocalDateTime.now(), 9.0));
        // 40 + 30 = 70 minutos >= 60: se completa para los dos
        org.assertj.core.api.Assertions.assertThat(resultado.at("/retosCompletados/0").asText()).isEqualTo("Minutos en equipo");
        get("/api/retos/mios", lider)
                .andExpect(jsonPath("$.respuesta.contenido[0].estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.respuesta.contenido[0].progreso").value(40))
                .andExpect(jsonPath("$.respuesta.contenido[0].progresoClan").value(70));
    }

    @Test
    void sePuedeAbandonarUnRetoEnCurso() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(post("/api/retos/" + idReto("Misión diaria") + "/participaciones", null, token)).at("/id").asLong();
        post("/api/retos/participaciones/" + id + "/abandono", null, token)
                .andExpect(jsonPath("$.respuesta.estado").value("ABANDONADO"));
        post("/api/retos/participaciones/" + id + "/abandono", null, token).andExpect(status().isBadRequest());
    }

    @Test
    void unRetoAbandonadoNoSePuedeRetomarEnElMismoPeriodo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long reto = idReto("Misión diaria");
        long id = respuesta(post("/api/retos/" + reto + "/participaciones", null, token)).at("/id").asLong();
        post("/api/retos/participaciones/" + id + "/abandono", null, token).andExpect(status().isOk());
        post("/api/retos/" + reto + "/participaciones", null, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value(org.hamcrest.Matchers.containsString("no puedes retomarlo")));
    }

    @Test
    void unRetoQueNoSeCompletaAntesDelLimiteQuedaVencidoYNoSeRepiteEnElPeriodo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long reto = idReto("Misión diaria");
        long id = respuesta(post("/api/retos/" + reto + "/participaciones", null, token)).at("/id").asLong();

        // pasa la fecha límite sin completar el reto
        var participacion = participacionRetoRepository.findById(id).orElseThrow();
        participacion.setFechaLimite(LocalDateTime.now().minusMinutes(1));
        participacion.setFechaInscripcion(LocalDateTime.now().minusHours(3));
        participacionRetoRepository.save(participacion);

        get("/api/retos/mios", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].estado").value("VENCIDO"));
        post("/api/retos/" + reto + "/participaciones", null, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value(org.hamcrest.Matchers.containsString("perdiste la oportunidad")));
    }

    @Test
    void elAdministradorCreaPublicaYFinalizaRetos() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        Map<String, Object> reto = new java.util.HashMap<>();
        reto.put("nombre", "Prueba semanal");
        reto.put("descripcion", "Sesiones esta semana");
        reto.put("metrica", "SESIONES");
        reto.put("valorMeta", 3);
        reto.put("xpRecompensa", 50);
        reto.put("periodicidad", "SEMANAL");
        reto.put("ambito", "INDIVIDUAL");
        reto.put("fechaInicio", LocalDateTime.now().minusDays(1).withNano(0));
        long id = respuesta(post("/api/admin/retos", reto, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("BORRADOR"))).at("/id").asLong();

        String jugador = registrar("leo@kinet.com", "leo_run");
        get("/api/retos", jugador).andExpect(jsonPath("$.respuesta.contenido[*].nombre", not(hasItem("Prueba semanal"))));
        patch("/api/admin/retos/" + id + "/estado", Map.of("estado", "ACTIVO"), admin).andExpect(status().isOk());
        get("/api/retos", jugador).andExpect(jsonPath("$.respuesta.contenido[*].nombre", hasItem("Prueba semanal")));
        put("/api/admin/retos/" + id, reto, admin).andExpect(status().isBadRequest());
        patch("/api/admin/retos/" + id + "/estado", Map.of("estado", "BORRADOR"), admin).andExpect(status().isBadRequest());
    }
}
