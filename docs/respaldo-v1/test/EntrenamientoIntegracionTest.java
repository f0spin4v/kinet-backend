package co.edu.uniquindio.kinet;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 2: registro, validación anti-trampa, XP, nivel, racha y notificaciones (RNF-02, RNF-24, RNF-27). */
class EntrenamientoIntegracionTest extends PruebaIntegracion {

    private static final LocalDateTime AYER = LocalDate.now().minusDays(1).atStartOfDay();

    @Test
    void lasActividadesSeListanConSuXp() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/actividades", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(3))
                .andExpect(jsonPath("$.respuesta[?(@.codigo=='RUNNING')].xpPorMinuto").value(2));
    }

    @Test
    void unEntrenamientoValidoDaXpRachaYPuedeSubirDeNivel() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");

        // 30 min de gimnasio (2 XP/min) a intensidad media (x1.5) = 90 XP
        entrenar(token, "GIMNASIO", 30, "MEDIA", haceHoras(2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.error").value(false))
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.xpOtorgado").value(90))
                .andExpect(jsonPath("$.respuesta.xpTotal").value(90))
                .andExpect(jsonPath("$.respuesta.nivel").value(1))
                .andExpect(jsonPath("$.respuesta.subioNivel").value(false))
                .andExpect(jsonPath("$.respuesta.rachaActual").value(1));

        // 40 min de running (2 XP/min) a intensidad alta (x2) = 160 XP; total 250 = nivel 3
        entrenar(token, "running", 40, "ALTA", haceHoras(5))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.xpGanado").value(160))
                .andExpect(jsonPath("$.respuesta.xpTotal").value(250))
                .andExpect(jsonPath("$.respuesta.nivel").value(3))
                .andExpect(jsonPath("$.respuesta.subioNivel").value(true));

        get("/api/progreso", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.xpTotal").value(250))
                .andExpect(jsonPath("$.respuesta.nivel").value(3))
                .andExpect(jsonPath("$.respuesta.xpInicioNivel").value(250))
                .andExpect(jsonPath("$.respuesta.xpSiguienteNivel").value(450))
                .andExpect(jsonPath("$.respuesta.entrenamientosSemana").value(2))
                .andExpect(jsonPath("$.respuesta.minutosSemana").value(70));
    }

    @Test
    void elAntiTrampaRechazaYExplicaElMotivo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");

        entrenar(token, "RUNNING", 5, "MEDIA", haceHoras(2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("al menos 10 minutos")))
                .andExpect(jsonPath("$.respuesta.xpGanado").value(0));
        entrenar(token, "RUNNING", 300, "MEDIA", haceHoras(10))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("más de 240")));
        entrenar(token, "RUNNING", 30, "MEDIA", LocalDateTime.now().plusHours(2))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("futuro")));
        entrenar(token, "RUNNING", 60, "MEDIA", LocalDateTime.now().minusMinutes(20))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("no ha terminado")));
        entrenar(token, "RUNNING", 30, "MEDIA", haceHoras(72))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("48 horas")));

        // dos sesiones que se cruzan en el horario
        LocalDateTime inicio = haceHoras(3);
        entrenar(token, "RUNNING", 60, "MEDIA", inicio)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"));
        entrenar(token, "GIMNASIO", 30, "MEDIA", inicio.plusMinutes(30))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("Se cruza")));

        assertThat(progresoRepository.findAll().get(0).getXpTotal()).isEqualTo(180);
    }

    @Test
    void hayTopesDeSesionesYMinutosPorDia() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        for (int hora : new int[]{6, 8, 10, 12}) {
            entrenar(token, "GIMNASIO", 20, "BAJA", AYER.plusHours(hora))
                    .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"));
        }
        entrenar(token, "GIMNASIO", 20, "BAJA", AYER.plusHours(14))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("máximo permitido")));

        String otro = registrar("ana@kinet.com", "ana_gg");
        entrenar(otro, "RUNNING", 120, "BAJA", AYER.plusHours(6)).andExpect(status().isCreated());
        entrenar(otro, "RUNNING", 120, "BAJA", AYER.plusHours(9)).andExpect(status().isCreated());
        entrenar(otro, "RUNNING", 70, "BAJA", AYER.plusHours(13))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("300 minutos")));
    }

    @Test
    void datosIncompletosOActividadInexistente() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        entrenar(token, "RUNNING", 30, "EXTREMA", haceHoras(2)).andExpect(status().isBadRequest());
        entrenar(token, "PARKOUR", 30, "MEDIA", haceHoras(2)).andExpect(status().isNotFound());
        post("/api/entrenamientos", Map.of("codigoActividad", "RUNNING"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta.length()").value(3));
    }

    @Test
    void elHistorialEsPrivadoDeCadaJugador() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String cuerpo = entrenar(token, "CICLISMO", 45, "MEDIA", haceHoras(2))
                .andReturn().getResponse().getContentAsString();
        long id = leer(cuerpo).at("/respuesta/entrenamiento/id").asLong();
        entrenar(token, "CICLISMO", 3, "MEDIA", haceHoras(6));

        get("/api/entrenamientos", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(2))
                .andExpect(jsonPath("$.respuesta[0].actividad").value("Ciclismo"));
        get("/api/entrenamientos/" + id, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.xpOtorgado").value(68));

        String otro = registrar("ana@kinet.com", "ana_gg");
        get("/api/entrenamientos/" + id, otro).andExpect(status().isNotFound());
    }

    @Test
    void cadaResultadoGeneraUnaNotificacionQueSePuedeMarcarLeida() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        entrenar(token, "RUNNING", 60, "ALTA", haceHoras(2)); // 240 XP: sube a nivel 2
        entrenar(token, "RUNNING", 5, "ALTA", haceHoras(6));

        String cuerpo = get("/api/notificaciones", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        JsonNode lista = leer(cuerpo).at("/respuesta");
        List<String> tipos = List.of(lista.get(0).get("tipo").asText(), lista.get(1).get("tipo").asText(),
                lista.get(2).get("tipo").asText());
        assertThat(tipos).contains("ENTRENAMIENTO_VALIDADO", "NIVEL_ALCANZADO", "ENTRENAMIENTO_RECHAZADO");

        long id = lista.get(0).get("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/notificaciones/" + id + "/leida").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.leida").value(true));
        get("/api/progreso", token).andExpect(jsonPath("$.respuesta.notificacionesSinLeer").value(2));
    }
}
