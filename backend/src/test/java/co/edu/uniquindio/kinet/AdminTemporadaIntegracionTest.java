package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida de una temporada desde administración: PROGRAMADA -> ACTIVA -> CERRADA. Siempre hay una sola
 * activa. (La foto final de puntos al cerrar está en CompetitivoIntegracionTest.)
 */
class AdminTemporadaIntegracionTest extends PruebaIntegracion {

    @Test
    void unaTemporadaNuevaQuedaProgramadaYSePuedeEditar() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        long id = respuesta(post("/api/admin/temporadas", temporada("Temporada 2", 60, 120, 40), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.respuesta.porcentajeConservado").value(40)))
                .at("/id").asLong();

        put("/api/admin/temporadas/" + id, temporada("Temporada 2: verano", 61, 150, 30), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.nombre").value("Temporada 2: verano"))
                .andExpect(jsonPath("$.respuesta.fechaFin").value(LocalDate.now().plusDays(150).toString()))
                .andExpect(jsonPath("$.respuesta.estado").value("PROGRAMADA"));
        get("/api/admin/temporadas", admin)
                .andExpect(jsonPath("$.respuesta.totalElementos").value(2));
    }

    @Test
    void lasFechasYElNombreSeValidan() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");

        post("/api/admin/temporadas", temporada("Al revés", 30, 10, 50), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("La fecha de fin debe ser posterior a la de inicio."));
        post("/api/admin/temporadas", temporada("Ya pasó", -60, -1, 50), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("La temporada no puede terminar en el pasado."));
        post("/api/admin/temporadas", temporada("Porcentaje alto", 60, 120, 101), admin)
                .andExpect(status().isBadRequest());
        post("/api/admin/temporadas", Map.of("nombre", "Sin fechas", "porcentajeConservado", 50), admin)
                .andExpect(status().isBadRequest());
        post("/api/admin/temporadas", temporada("Temporada de prueba", 60, 120, 50), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value("Ya existe una temporada con ese nombre."));
    }

    @Test
    void soloHayUnaTemporadaActivaALaVez() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        long actual = temporadaRepository.findAll().get(0).getId();
        long siguiente = respuesta(post("/api/admin/temporadas", temporada("Temporada 2", 1, 90, 50), admin)).at("/id").asLong();

        post("/api/admin/temporadas/" + siguiente + "/activacion", null, admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.respuesta").value("Ya está activa la temporada \"Temporada de prueba\". Ciérrala primero."));

        post("/api/admin/temporadas/" + actual + "/cierre", null, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("CERRADA"));
        post("/api/admin/temporadas/" + siguiente + "/activacion", null, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("ACTIVA"));

        String jugador = registrar("leo@kinet.com", "leo_run");
        get("/api/temporadas/activa", jugador).andExpect(jsonPath("$.respuesta.nombre").value("Temporada 2"));
    }

    @Test
    void cadaEstadoSoloAdmiteSuSiguientePaso() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        long activa = temporadaRepository.findAll().get(0).getId();
        long programada = respuesta(post("/api/admin/temporadas", temporada("Temporada 2", 1, 90, 50), admin)).at("/id").asLong();

        // una activa no se edita ni se vuelve a activar
        put("/api/admin/temporadas/" + activa, temporada("Cambio", 1, 90, 50), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Solo se puede editar una temporada programada."));
        post("/api/admin/temporadas/" + activa + "/activacion", null, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Solo se puede activar una temporada programada."));
        // una programada todavía no se cierra
        post("/api/admin/temporadas/" + programada + "/cierre", null, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Solo se puede cerrar la temporada activa."));
        // una cerrada no vuelve atrás
        post("/api/admin/temporadas/" + activa + "/cierre", null, admin).andExpect(status().isOk());
        post("/api/admin/temporadas/" + activa + "/cierre", null, admin).andExpect(status().isBadRequest());
        post("/api/admin/temporadas/" + activa + "/activacion", null, admin).andExpect(status().isBadRequest());

        post("/api/admin/temporadas/999999/activacion", null, admin).andExpect(status().isNotFound());
    }

    @Test
    void soloElAdministradorGestionaTemporadas() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        long activa = temporadaRepository.findAll().get(0).getId();

        for (String token : new String[]{jugador, entrenador}) {
            get("/api/admin/temporadas", token).andExpect(status().isForbidden());
            post("/api/admin/temporadas", temporada("Intrusa", 1, 90, 50), token).andExpect(status().isForbidden());
            post("/api/admin/temporadas/" + activa + "/cierre", null, token).andExpect(status().isForbidden());
        }
    }

    /** Fechas relativas a hoy, en días. */
    private static Map<String, Object> temporada(String nombre, int inicio, int fin, int porcentaje) {
        return Map.of("nombre", nombre,
                "fechaInicio", LocalDate.now().plusDays(inicio).toString(),
                "fechaFin", LocalDate.now().plusDays(fin).toString(),
                "porcentajeConservado", porcentaje);
    }
}
