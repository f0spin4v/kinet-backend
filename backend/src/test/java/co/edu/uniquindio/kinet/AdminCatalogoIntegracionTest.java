package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.NivelProgresion;
import co.edu.uniquindio.kinet.repositories.NivelProgresionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administración del catálogo: actividades, niveles de progresión y cuestionario de evaluación.
 * Estos catálogos los comparten todas las pruebas, así que cada cambio se deshace al terminar.
 */
class AdminCatalogoIntegracionTest extends PruebaIntegracion {

    private static final Set<String> ACTIVIDADES_BASE = Set.of("RUNNING", "CICLISMO", "FUERZA", "CALISTENIA");
    private static final Map<Integer, Integer> XP_NIVELES_BASE = Map.of(1, 0, 2, 100, 3, 250, 4, 450, 5, 700, 6, 1000);

    @Autowired private NivelProgresionRepository nivelRepository;

    @AfterEach
    void restaurarCatalogos() {
        actividadRepository.findAll().stream()
                .filter(a -> !ACTIVIDADES_BASE.contains(a.getNombre()))
                .forEach(actividadRepository::delete);
        preguntaRepository.findAll().stream()
                .filter(p -> p.getOrden() > 3)
                .forEach(preguntaRepository::delete);
        preguntaRepository.findAll().forEach(p -> {
            p.setActiva(true);
            preguntaRepository.save(p);
        });
        // save() limpia la caché de niveles (NivelProgresionRepository)
        for (NivelProgresion nivel : nivelRepository.findAll()) {
            Integer xp = XP_NIVELES_BASE.get(nivel.getNumeroNivel());
            if (xp != null && !xp.equals(nivel.getXpRequerido())) {
                nivel.setXpRequerido(xp);
                nivelRepository.save(nivel);
            }
        }
    }

    // ---------- actividades ----------

    @Test
    void elAdministradorCreaYRenombraActividades() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        long id = respuesta(post("/api/admin/actividades", Map.of("nombre", "  natacion libre "), admin)
                .andExpect(status().isCreated())
                // el nombre se normaliza: mayúsculas y guion bajo
                .andExpect(jsonPath("$.respuesta.nombre").value("NATACION_LIBRE"))
                .andExpect(jsonPath("$.respuesta.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.respuesta.reglasVigentes.length()").value(0)))
                .at("/id").asLong();

        put("/api/admin/actividades/" + id, Map.of("nombre", "Natacion"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombre").value("NATACION"));
        get("/api/admin/actividades", admin)
                .andExpect(jsonPath("$.respuesta[?(@.nombre=='NATACION')].id").value((int) id));
    }

    @Test
    void elNombreDeUnaActividadEsUnicoYSoloLlevaLetras() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        post("/api/admin/actividades", Map.of("nombre", "running"), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value("Ya existe una actividad con ese nombre."));
        post("/api/admin/actividades", Map.of("nombre", ""), admin).andExpect(status().isBadRequest());
        post("/api/admin/actividades", Map.of("nombre", "Yoga 2"), admin).andExpect(status().isBadRequest());
        post("/api/admin/actividades", Map.of("nombre", "a".repeat(41)), admin).andExpect(status().isBadRequest());

        long id = respuesta(post("/api/admin/actividades", Map.of("nombre", "Yoga"), admin)).at("/id").asLong();
        put("/api/admin/actividades/" + id, Map.of("nombre", "Fuerza"), admin).andExpect(status().isConflict());
        put("/api/admin/actividades/999999", Map.of("nombre", "Pilates"), admin).andExpect(status().isNotFound());
    }

    @Test
    void unaActividadNuevaApareceEnElCatalogoDeLosJugadores() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String jugador = registrar("leo@kinet.com", "leo_run");

        post("/api/admin/actividades", Map.of("nombre", "Remo"), admin).andExpect(status().isCreated());

        get("/api/catalogos/actividades", jugador)
                .andExpect(jsonPath("$.respuesta[?(@.nombre=='REMO')]").isNotEmpty());
    }

    // ---------- niveles de progresión ----------

    @Test
    void elXpDeUnNivelSeEditaManteniendoLaTablaCreciente() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        put("/api/admin/niveles-progresion/2", Map.of("xpRequerido", 120), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.xpRequerido").value(120));
        get("/api/catalogos/niveles-progresion", admin)
                .andExpect(jsonPath("$.respuesta[1].xpRequerido").value(120));

        put("/api/admin/niveles-progresion/1", Map.of("xpRequerido", 10), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("El nivel 1 empieza en 0 XP."));
        put("/api/admin/niveles-progresion/3", Map.of("xpRequerido", 120), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Debe pedir más XP que el nivel 2."));
        put("/api/admin/niveles-progresion/3", Map.of("xpRequerido", 450), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Debe pedir menos XP que el nivel 4."));
        put("/api/admin/niveles-progresion/3", Map.of("xpRequerido", -1), admin).andExpect(status().isBadRequest());
        put("/api/admin/niveles-progresion/99", Map.of("xpRequerido", 5000), admin).andExpect(status().isNotFound());
    }

    // ---------- cuestionario ----------

    @Test
    void unaPreguntaNuevaEntraAlCuestionarioYHayQueResponderla() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String jugador = registrar("leo@kinet.com", "leo_run");
        List<Long> respuestasViejas = preguntaRepository.findByActivaOrderByOrdenAsc(true).stream()
                .map(p -> p.getOpciones().get(0).getId())
                .toList();

        post("/api/admin/preguntas", pregunta("¿Cuántos días entrenas?", 4), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.activa").value(true))
                .andExpect(jsonPath("$.respuesta.opciones.length()").value(2));

        get("/api/evaluacion/preguntas", jugador).andExpect(jsonPath("$.respuesta.length()").value(4));
        // con las respuestas de antes ya no alcanza
        post("/api/evaluacion", Map.of("idsOpciones", respuestasViejas), jugador)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("4 preguntas")));
    }

    @Test
    void lasPreguntasSeValidanAlCrearlas() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        post("/api/admin/preguntas", pregunta("Otra en la posición 1", 1), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value("Ya hay una pregunta en la posición 1."));
        post("/api/admin/preguntas", Map.of("enunciado", "Una sola opción", "orden", 5,
                "opciones", List.of(opcion("Sí", "INICIACION"))), admin)
                .andExpect(status().isBadRequest());
        post("/api/admin/preguntas", Map.of("enunciado", "Dos opciones al mismo nivel", "orden", 5,
                "opciones", List.of(opcion("A", "AVANZADO"), opcion("B", "AVANZADO"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Cada opción de una pregunta debe votar por un nivel distinto."));
        post("/api/admin/preguntas", pregunta("Orden fuera de rango", 100), admin).andExpect(status().isBadRequest());
    }

    @Test
    void unaPreguntaSinRespuestasSeEditaCompleta() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        long id = respuesta(post("/api/admin/preguntas", pregunta("Borrador", 4), admin)).at("/id").asLong();

        put("/api/admin/preguntas/" + id, Map.of("enunciado", "Versión final", "orden", 5,
                "opciones", List.of(opcion("Poco", "INICIACION"), opcion("Algo", "INTERMEDIO"), opcion("Mucho", "ATLETA"))),
                admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.enunciado").value("Versión final"))
                .andExpect(jsonPath("$.respuesta.orden").value(5))
                .andExpect(jsonPath("$.respuesta.opciones.length()").value(3));
        put("/api/admin/preguntas/" + id, pregunta("A la posición 2", 2), admin).andExpect(status().isConflict());
    }

    @Test
    void unaPreguntaRespondidaNoCambiaSusOpciones() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String jugador = registrar("leo@kinet.com", "leo_run");
        evaluar(jugador, "INTERMEDIO");
        long id = preguntaRepository.findByActivaOrderByOrdenAsc(true).get(0).getId();

        put("/api/admin/preguntas/" + id, pregunta("Opciones nuevas", 1), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("ya tiene respuestas")));
    }

    @Test
    void unaPreguntaDesactivadaSaleDelCuestionarioPeroSiempreQuedaUna() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String jugador = registrar("leo@kinet.com", "leo_run");
        List<Long> ids = preguntaRepository.findByActivaOrderByOrdenAsc(true).stream().map(p -> p.getId()).toList();

        patch("/api/admin/preguntas/" + ids.get(0) + "/estado", Map.of("activa", false), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.activa").value(false));
        get("/api/evaluacion/preguntas", jugador).andExpect(jsonPath("$.respuesta.length()").value(2));
        // el administrador la sigue viendo para poder reactivarla
        get("/api/admin/preguntas", admin).andExpect(jsonPath("$.respuesta.length()").value(3));

        patch("/api/admin/preguntas/" + ids.get(1) + "/estado", Map.of("activa", false), admin).andExpect(status().isOk());
        patch("/api/admin/preguntas/" + ids.get(2) + "/estado", Map.of("activa", false), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("El cuestionario debe tener al menos una pregunta activa."));
    }

    // ---------- permisos ----------

    @Test
    void soloElAdministradorGestionaElCatalogo() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");

        for (String token : new String[]{jugador, entrenador}) {
            get("/api/admin/actividades", token).andExpect(status().isForbidden());
            post("/api/admin/actividades", Map.of("nombre", "Remo"), token).andExpect(status().isForbidden());
            put("/api/admin/niveles-progresion/2", Map.of("xpRequerido", 120), token).andExpect(status().isForbidden());
            get("/api/admin/preguntas", token).andExpect(status().isForbidden());
            post("/api/admin/preguntas", pregunta("Intrusa", 4), token).andExpect(status().isForbidden());
        }
    }

    private static Map<String, Object> pregunta(String enunciado, int orden) {
        return Map.of("enunciado", enunciado, "orden", orden,
                "opciones", List.of(opcion("Poco", "INICIACION"), opcion("Mucho", "AVANZADO")));
    }

    private static Map<String, Object> opcion(String titulo, String nivel) {
        return Map.of("titulo", titulo, "descripcion", "Opción de prueba", "nivelHabilidad", nivel, "puntaje", 1);
    }
}
