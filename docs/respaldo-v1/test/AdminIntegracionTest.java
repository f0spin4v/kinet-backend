package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Rol;
import co.edu.uniquindio.kinet.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El administrador configura las reglas del Proceso 2; un jugador no puede hacerlo. */
class AdminIntegracionTest extends PruebaIntegracion {

    private String tokenAdmin() throws Exception {
        registrar("admin@kinet.com", "admin_kinet");
        Usuario admin = usuarioRepository.findByCorreo("admin@kinet.com").orElseThrow();
        admin.setRol(rolRepository.findByNombre(Rol.ADMINISTRADOR).orElseThrow());
        usuarioRepository.save(admin);
        return iniciarSesion("admin@kinet.com", "Segura123");
    }

    private Map<String, Object> reto() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("titulo", "Prueba nado");
        datos.put("descripcion", "Reto creado en las pruebas.");
        datos.put("tipoMeta", "SESIONES");
        datos.put("metaValor", 2);
        datos.put("codigoActividad", "running");
        datos.put("nivelRequerido", "AVANZADO");
        datos.put("duracionDias", 5);
        datos.put("xpRecompensa", 40);
        return datos;
    }

    @Test
    void unJugadorNoEntraAAdministracion() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        get("/api/admin/retos", jugador)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.respuesta").value("No tienes permiso para hacer esta acción."));
    }

    @Test
    void elAdministradorCreaYDesactivaRetos() throws Exception {
        String admin = tokenAdmin();

        String cuerpo = post("/api/admin/retos", reto(), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.codigoActividad").value("RUNNING"))
                .andExpect(jsonPath("$.respuesta.nivelRequerido").value("AVANZADO"))
                .andExpect(jsonPath("$.respuesta.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = leer(cuerpo).at("/respuesta/id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/retos/" + id + "/estado").param("activo", "false")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.activo").value(false));
        get("/api/admin/retos", admin).andExpect(status().isOk());

        Map<String, Object> malo = reto();
        malo.put("metaValor", 0);
        post("/api/admin/retos", malo, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta[0].campo").value("metaValor"));
    }

    @Test
    void elAdministradorAjustaElXpDeUnaActividad() throws Exception {
        String admin = tokenAdmin();
        try {
            put("/api/admin/actividades/CICLISMO", Map.of("xpPorMinuto", 3), admin)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.respuesta.xpPorMinuto").value(3));
            put("/api/admin/actividades/PARKOUR", Map.of("xpPorMinuto", 3), admin).andExpect(status().isNotFound());
        } finally {
            put("/api/admin/actividades/CICLISMO", Map.of("xpPorMinuto", 1), admin);
        }
    }
}
