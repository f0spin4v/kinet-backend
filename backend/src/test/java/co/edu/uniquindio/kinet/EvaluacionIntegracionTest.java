package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Proceso 1: evaluación de nivel inicial (UI-001). */
class EvaluacionIntegracionTest extends PruebaIntegracion {

    @Test
    void lasPreguntasNoRevelanElPuntaje() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/evaluacion/preguntas", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.length()").value(5))
                .andExpect(jsonPath("$.respuesta[2].opciones.length()").value(2))
                .andExpect(jsonPath("$.respuesta[0].opciones[0].puntos").doesNotExist());
    }

    @Test
    void enviarLaEvaluacionGuardaElNivelYApareceEnElUsuario() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        get("/api/evaluacion", token).andExpect(status().isNotFound());

        post("/api/evaluacion", Map.of("respuestas", List.of("B", "B", "A", "B", "A")), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.puntaje").value(3))
                .andExpect(jsonPath("$.respuesta.nivelInicial").value("INTERMEDIO"));

        get("/api/usuarios/me", token).andExpect(jsonPath("$.respuesta.nivelInicial").value("INTERMEDIO"));
        get("/api/evaluacion", token).andExpect(jsonPath("$.respuesta.puntaje").value(3));
        post("/api/auth/login", Map.of("correo", "leo@kinet.com", "contrasena", "Segura123"))
                .andExpect(jsonPath("$.respuesta.usuario.nivelInicial").value("INTERMEDIO"));
    }

    @Test
    void laEvaluacionSoloSeHaceUnaVezYValidaLasRespuestas() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/evaluacion", Map.of("respuestas", List.of("A", "B")), token).andExpect(status().isBadRequest());
        post("/api/evaluacion", Map.of("respuestas", List.of("A", "B", "C", "A", "B")), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta[0].campo").value("respuestas"));

        evaluar(token, List.of("A", "A", "A", "A", "A"));
        post("/api/evaluacion", Map.of("respuestas", List.of("B", "B", "B", "B", "B")), token)
                .andExpect(status().isConflict());
        assertThat(evaluacionRepository.count()).isEqualTo(1);
    }
}
