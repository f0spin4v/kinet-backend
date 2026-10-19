package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.EvaluacionUsuario;
import co.edu.uniquindio.kinet.model.PreguntaEvaluacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Placement: voto ponderado por nivel, empate conservador y repetición cada 30 días. */
class EvaluacionIntegracionTest extends PruebaIntegracion {

    @Test
    void lasPreguntasNoRevelanAQueNivelVotaCadaOpcion() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        get("/api/evaluacion/preguntas", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(3))
                .andExpect(jsonPath("$.respuesta[0].opciones.length()").value(4))
                .andExpect(jsonPath("$.respuesta[0].opciones[0].nivelHabilidad").doesNotExist())
                .andExpect(jsonPath("$.respuesta[0].opciones[0].puntaje").doesNotExist());
    }

    @Test
    void ganaElNivelMasVotadoYLaPreguntaPesadaCuentaDoble() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        // P1 -> INICIACION (1), P2 -> AVANZADO (2), P3 -> INICIACION (1): empate 2-2, gana el de menor orden
        post("/api/evaluacion", Map.of("idsOpciones", opciones("INICIACION", "AVANZADO", "INICIACION")), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.nivelHabilidad").value("INICIACION"))
                .andExpect(jsonPath("$.respuesta.votos.INICIACION").value(2))
                .andExpect(jsonPath("$.respuesta.votos.AVANZADO").value(2))
                .andExpect(jsonPath("$.respuesta.versionAlgoritmo").value("VOTO_PONDERADO_V1"));

        String otro = registrar("leo@kinet.com", "leo_run");
        post("/api/evaluacion", Map.of("idsOpciones", opciones("INTERMEDIO", "ATLETA", "ATLETA")), otro)
                .andExpect(jsonPath("$.respuesta.nivelHabilidad").value("ATLETA"));
        get("/api/usuarios/me", otro).andExpect(jsonPath("$.respuesta.nivelHabilidad").value("ATLETA"));
    }

    @Test
    void hayQueResponderCadaPreguntaUnaVez() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        List<Long> dos = opciones("INICIACION", "INICIACION", "INICIACION").subList(0, 2);
        post("/api/evaluacion", Map.of("idsOpciones", dos), token).andExpect(status().isBadRequest());
        post("/api/evaluacion", Map.of("idsOpciones", List.of(999999L)), token).andExpect(status().isNotFound());
    }

    @Test
    void laEvaluacionSeRepiteCadaTreintaDias() throws Exception {
        String token = registrar("ana@kinet.com", "ana_gg");
        evaluar(token, "INTERMEDIO");
        post("/api/evaluacion", Map.of("idsOpciones", opciones("AVANZADO", "AVANZADO", "AVANZADO")), token)
                .andExpect(status().isBadRequest());

        EvaluacionUsuario anterior = evaluacionRepository.findAll().get(0);
        anterior.setFechaEvaluacion(anterior.getFechaEvaluacion().minusDays(31));
        evaluacionRepository.save(anterior);
        post("/api/evaluacion", Map.of("idsOpciones", opciones("AVANZADO", "AVANZADO", "AVANZADO")), token)
                .andExpect(status().isCreated());
        get("/api/evaluacion", token).andExpect(jsonPath("$.respuesta.nivelHabilidad").value("AVANZADO"));
        get("/api/evaluacion/historial", token).andExpect(jsonPath("$.respuesta.contenido.length()").value(2));
    }

    private List<Long> opciones(String... niveles) {
        List<PreguntaEvaluacion> preguntas = preguntaRepository.findByActivaOrderByOrdenAsc(true);
        return java.util.stream.IntStream.range(0, niveles.length)
                .mapToObj(i -> preguntas.get(i).getOpciones().stream()
                        .filter(o -> o.getNivelHabilidad().getNombre().equals(niveles[i]))
                        .findFirst().orElseThrow().getId())
                .toList();
    }
}
