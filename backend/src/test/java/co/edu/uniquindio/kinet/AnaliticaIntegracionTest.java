package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Indicadores del administrador: conteos que cuadran con lo que pasó y ventanas de 30 días. */
class AnaliticaIntegracionTest extends PruebaIntegracion {

    @Test
    void elResumenCuentaUsuariosPorEstadoRolYNivel() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String leo = registrar("leo@kinet.com", "leo_run");
        registrar("ana@kinet.com", "ana_gg");
        cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        evaluar(leo, "AVANZADO");
        patch("/api/soporte/usuarios/" + idDe("ana@kinet.com") + "/estado", Map.of("estado", "BLOQUEADO"), admin)
                .andExpect(status().isOk());

        get("/api/analitica/resumen", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.usuariosPorEstado.ACTIVO").value(3))
                .andExpect(jsonPath("$.respuesta.usuariosPorEstado.BLOQUEADO").value(1))
                .andExpect(jsonPath("$.respuesta.usuariosPorRol.JUGADOR").value(2))
                .andExpect(jsonPath("$.respuesta.usuariosPorRol.ENTRENADOR").value(1))
                .andExpect(jsonPath("$.respuesta.usuariosPorRol.ADMINISTRADOR").value(1))
                .andExpect(jsonPath("$.respuesta.jugadoresPorNivelHabilidad.AVANZADO").value(1));
    }

    @Test
    void losEntrenamientosSoloCuentanLosUltimosTreintaDias() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String leo = registrar("leo@kinet.com", "leo_run");

        entrenar(leo, "FUERZA", 30, "MEDIA", haceHoras(24 * 40), null).andExpect(status().isOk()); // fuera de la ventana
        entrenar(leo, "FUERZA", 30, "MEDIA", haceHoras(3), null).andExpect(status().isOk());
        entrenar(leo, "CICLISMO", 45, "BAJA", haceHoras(6), 15.0).andExpect(status().isOk());
        entrenar(leo, "FUERZA", 5, "BAJA", haceHoras(10), null); // el anti-trampa lo rechaza

        get("/api/analitica/resumen", admin)
                .andExpect(jsonPath("$.respuesta.entrenamientosPorEstado30Dias.VALIDADO").value(2))
                .andExpect(jsonPath("$.respuesta.entrenamientosPorEstado30Dias.RECHAZADO").value(1))
                .andExpect(jsonPath("$.respuesta.minutosPorActividad30Dias.FUERZA").value(30))
                .andExpect(jsonPath("$.respuesta.minutosPorActividad30Dias.CICLISMO").value(45))
                .andExpect(jsonPath("$.respuesta.xpPorOrigen30Dias.ENTRENAMIENTO").value(greaterThan(0)));
    }

    @Test
    void elResumenNoExponeDatosPersonales() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        registrar("leo@kinet.com", "leo_run");

        get("/api/analitica/resumen", admin)
                .andExpect(content().string(not(containsString("leo@kinet.com"))))
                .andExpect(content().string(not(containsString("leo_run"))));
    }

    @Test
    void soloElAdministradorVeLaAnalitica() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");

        get("/api/analitica/resumen", jugador).andExpect(status().isForbidden());
        get("/api/analitica/resumen", entrenador).andExpect(status().isForbidden());
        get("/api/analitica/resumen", null).andExpect(status().isUnauthorized());
    }
}
