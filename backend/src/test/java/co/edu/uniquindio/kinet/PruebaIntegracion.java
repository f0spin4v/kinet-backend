package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: levanta la aplicación completa con H2 en modo Oracle,
 * limpia los datos de jugadores antes de cada prueba y ofrece atajos para las peticiones comunes.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaIntegracion {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected EvaluacionNivelRepository evaluacionRepository;
    @Autowired protected ProgresoJugadorRepository progresoRepository;
    @Autowired protected EntrenamientoRepository entrenamientoRepository;
    @Autowired protected ParticipacionRetoRepository participacionRepository;
    @Autowired protected NotificacionRepository notificacionRepository;
    @Autowired protected TokenRecuperacionRepository tokenRepository;
    @Autowired protected RetoRepository retoRepository;

    @BeforeEach
    void limpiarDatos() {
        notificacionRepository.deleteAll();
        participacionRepository.deleteAll();
        entrenamientoRepository.deleteAll();
        progresoRepository.deleteAll();
        tokenRepository.deleteAll();
        evaluacionRepository.deleteAll();
        usuarioRepository.deleteAll();
        retoRepository.findAll().stream()
                .filter(r -> r.getTitulo().startsWith("Prueba"))
                .forEach(retoRepository::delete);
    }

    // ---------- atajos ----------

    protected Map<String, Object> datosRegistro(String correo, String nickname) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Jugador de Prueba");
        datos.put("correo", correo);
        datos.put("contrasena", "Segura123");
        datos.put("nickname", nickname);
        datos.put("aceptaDatos", true);
        return datos;
    }

    protected ResultActions post(String url, Object cuerpo) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    protected ResultActions post(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    protected ResultActions put(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token));
    }

    /** Registra un jugador y devuelve su token. */
    protected String registrar(String correo, String nickname) throws Exception {
        String cuerpo = post("/api/auth/registro", datosRegistro(correo, nickname))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).at("/respuesta/token").asText();
    }

    protected String iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = post("/api/auth/login", Map.of("correo", correo, "contrasena", contrasena))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).at("/respuesta/token").asText();
    }

    /** Hace la evaluación de nivel. Todas A: Iniciación. Todas B: Avanzado. */
    protected void evaluar(String token, List<String> respuestas) throws Exception {
        post("/api/evaluacion", Map.of("respuestas", respuestas), token).andExpect(status().isCreated());
    }

    protected ResultActions entrenar(String token, String actividad, int minutos, String intensidad,
                                     LocalDateTime inicio) throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigoActividad", actividad);
        datos.put("duracionMinutos", minutos);
        datos.put("intensidad", intensidad);
        datos.put("fechaInicio", inicio.truncatedTo(ChronoUnit.SECONDS));
        return post("/api/entrenamientos", datos, token);
    }

    protected JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }

    protected static LocalDateTime haceHoras(long horas) {
        return LocalDateTime.now().minusHours(horas);
    }
}
