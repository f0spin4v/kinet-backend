package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.Usuario;
import co.edu.uniquindio.kinet.model.enums.EstadoActividad;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import co.edu.uniquindio.kinet.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: levanta la aplicación completa con H2 en modo Oracle, deja los datos de
 * juego en blanco antes de cada prueba (los catálogos de data.sql se conservan) y ofrece atajos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojDePrueba.Config.class)
public abstract class PruebaIntegracion {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected PerfilJugadorRepository perfilRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected TokenRecuperacionRepository tokenRepository;
    @Autowired protected EvaluacionUsuarioRepository evaluacionRepository;
    @Autowired protected PreguntaEvaluacionRepository preguntaRepository;
    @Autowired protected EntrenamientoRepository entrenamientoRepository;
    @Autowired protected EvidenciaEntrenamientoRepository evidenciaRepository;
    @Autowired protected RelojDePrueba reloj;
    @Autowired protected MovimientoXpRepository movimientoRepository;
    @Autowired protected NotificacionRepository notificacionRepository;
    @Autowired protected RetoRepository retoRepository;
    @Autowired protected ParticipacionRetoRepository participacionRetoRepository;
    @Autowired protected AporteRetoRepository aporteRepository;
    @Autowired protected ClanRepository clanRepository;
    @Autowired protected MembresiaClanRepository membresiaRepository;
    @Autowired protected TemporadaRepository temporadaRepository;
    @Autowired protected ParticipacionTemporadaRepository participacionTemporadaRepository;
    @Autowired protected EnfrentamientoRepository enfrentamientoRepository;
    @Autowired protected SolicitudMatchmakingRepository solicitudRepository;
    @Autowired protected LogroRepository logroRepository;
    @Autowired protected LogroUsuarioRepository logroUsuarioRepository;
    @Autowired protected ReglaPuntuacionRepository reglaRepository;
    @Autowired protected TipoActividadRepository actividadRepository;
    @Autowired protected HistorialRangoRepository historialRangoRepository;
    @Autowired protected CompetenciaRepository competenciaRepository;

    @BeforeEach
    protected void limpiarDatos() {
        reloj.liberar();
        notificacionRepository.deleteAll();
        movimientoRepository.deleteAll();
        competenciaRepository.deleteAll();
        evidenciaRepository.deleteAll();
        logroUsuarioRepository.deleteAll();
        aporteRepository.deleteAll();
        participacionRetoRepository.deleteAll();
        solicitudRepository.deleteAll();
        enfrentamientoRepository.deleteAll();
        historialRangoRepository.deleteAll();
        participacionTemporadaRepository.deleteAll();
        membresiaRepository.deleteAll();
        clanRepository.deleteAll();
        entrenamientoRepository.deleteAll();
        evaluacionRepository.deleteAll();
        tokenRepository.deleteAll();
        perfilRepository.deleteAll();
        usuarioRepository.deleteAll();
        // catálogos que algunas pruebas modifican
        actividadRepository.findAll().forEach(a -> {
            a.setEstado(EstadoActividad.ACTIVA);
            actividadRepository.save(a);
        });
        retoRepository.findAll().stream().filter(r -> r.getNombre().startsWith("Prueba")).forEach(retoRepository::delete);
        logroRepository.findAll().stream().filter(l -> l.getNombre().startsWith("Prueba")).forEach(logroRepository::delete);
        reglaRepository.findAll().stream().filter(r -> r.getVigenteDesde().isAfter(LocalDate.of(2026, 1, 1)))
                .forEach(reglaRepository::delete);
        reglaRepository.findAll().forEach(r -> {
            r.setVigenteHasta(null);
            reglaRepository.save(r);
        });
        temporadaRepository.findAll().forEach(t -> {
            if (t.getNombre().equals("Temporada de prueba")) {
                t.setEstado(EstadoTemporada.ACTIVA);
                temporadaRepository.save(t);
            } else {
                temporadaRepository.delete(t);
            }
        });
    }

    // ---------- cuentas ----------

    protected Map<String, Object> datosRegistro(String correo, String gamertag) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", "Jugador");
        datos.put("apellidos", "De Prueba");
        datos.put("correo", correo);
        datos.put("contrasena", "Segura123");
        datos.put("gamertag", gamertag);
        datos.put("aceptaPolitica", true);
        return datos;
    }

    /** Registra un jugador y devuelve su token. */
    protected String registrar(String correo, String gamertag) throws Exception {
        String cuerpo = post("/api/auth/registro", datosRegistro(correo, gamertag), null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).at("/respuesta/token").asText();
    }

    protected String iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = post("/api/auth/login", Map.of("correo", correo, "contrasena", contrasena), null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return leer(cuerpo).at("/respuesta/token").asText();
    }

    /** Crea una cuenta con un rol del equipo (se registra como jugador y se le cambia el rol) y devuelve su token. */
    protected String cuentaConRol(String correo, String gamertag, String rol) throws Exception {
        registrar(correo, gamertag);
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseThrow();
        Rol nuevo = rolRepository.findByNombre(rol).orElseThrow();
        usuario.setRol(nuevo);
        usuarioRepository.save(usuario);
        return iniciarSesion(correo, "Segura123");
    }

    protected Long idDe(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow().getId();
    }

    protected Long idActividad(String nombre) {
        return actividadRepository.findByNombre(nombre).orElseThrow().getId();
    }

    protected Long idReto(String nombre) {
        return retoRepository.findAll().stream().filter(r -> r.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }

    /** Hace la evaluación eligiendo, en cada pregunta, la opción que vota por el nivel dado. */
    protected void evaluar(String token, String nivel) throws Exception {
        List<Long> opciones = preguntaRepository.findByActivaOrderByOrdenAsc(true).stream()
                .map(p -> p.getOpciones().stream().filter(o -> o.getNivelHabilidad().getNombre().equals(nivel))
                        .findFirst().orElseThrow().getId())
                .toList();
        post("/api/evaluacion", Map.of("idsOpciones", opciones), token).andExpect(status().isCreated());
    }

    // ---------- entrenamientos ----------

    /**
     * Usa el cronómetro como lo haría la app: "Iniciar" con el reloj del servidor en "inicio" y "Finalizar"
     * "minutos" después. Devuelve la respuesta de finalizar.
     */
    protected ResultActions entrenar(String token, String actividad, int minutos, String intensidad,
                                     LocalDateTime inicio, Double km) throws Exception {
        LocalDateTime desde = inicio.truncatedTo(ChronoUnit.SECONDS);
        reloj.fijar(desde);
        iniciar(token, actividad).andExpect(status().isCreated());
        reloj.fijar(desde.plusMinutes(minutos));
        try {
            return finalizar(token, intensidad, km);
        } finally {
            reloj.liberar();
        }
    }

    private static final AtomicLong ACTIVIDADES_STRAVA = new AtomicLong(9_000_000);

    /**
     * Igual que entrenar, pero sube enseguida el enlace de la actividad: así el entrenamiento cuenta en
     * enfrentamientos y competencias. Devuelve la respuesta de finalizar.
     */
    protected ResultActions entrenarConEvidencia(String token, String actividad, int minutos, String intensidad,
                                                 LocalDateTime inicio, Double km) throws Exception {
        ResultActions resultado = entrenar(token, actividad, minutos, intensidad, inicio, km);
        long id = respuesta(resultado).at("/entrenamiento/id").asLong();
        subirEnlace(token, id, "https://www.strava.com/activities/" + ACTIVIDADES_STRAVA.incrementAndGet())
                .andExpect(status().isCreated());
        return resultado;
    }

    protected ResultActions subirEnlace(String token, long idEntrenamiento, String url) throws Exception {
        return post("/api/entrenamientos/" + idEntrenamiento + "/evidencia/enlace", Map.of("url", url), token);
    }

    protected ResultActions iniciar(String token, String actividad) throws Exception {
        return post("/api/entrenamientos/en-curso", Map.of("idTipoActividad", idActividad(actividad)), token);
    }

    protected ResultActions finalizar(String token, String intensidad, Double km) throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("intensidad", intensidad);
        datos.put("distanciaKm", km);
        return post("/api/entrenamientos/en-curso/fin", datos, token);
    }

    protected static LocalDateTime haceHoras(long horas) {
        return LocalDateTime.now().minusHours(horas);
    }

    // ---------- peticiones ----------

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(conToken(MockMvcRequestBuilders.get(url), token));
    }

    protected ResultActions post(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(MockMvcRequestBuilders.post(url), cuerpo, token));
    }

    protected ResultActions put(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(MockMvcRequestBuilders.put(url), cuerpo, token));
    }

    protected ResultActions patch(String url, Object cuerpo, String token) throws Exception {
        return mvc.perform(conCuerpo(MockMvcRequestBuilders.patch(url), cuerpo, token));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(conToken(MockMvcRequestBuilders.delete(url), token));
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, Object cuerpo, String token)
            throws Exception {
        peticion = conToken(peticion, token);
        return cuerpo == null ? peticion
                : peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return token == null ? peticion : peticion.header("Authorization", "Bearer " + token);
    }

    protected JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }

    protected JsonNode respuesta(ResultActions resultado) throws Exception {
        return leer(resultado.andReturn().getResponse().getContentAsString()).at("/respuesta");
    }
}
