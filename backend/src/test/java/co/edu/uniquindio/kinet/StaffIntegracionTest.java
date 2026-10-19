package co.edu.uniquindio.kinet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Permisos por rol (jugador, entrenador, administrador) y funciones de administración, soporte y analítica. */
class StaffIntegracionTest extends PruebaIntegracion {

    @Test
    void laListaSePaginaConOrdenEstableYTamanioAcotado() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        for (int i = 0; i < 4; i++) {
            registrar("j" + i + "@kinet.com", "jugador_" + i);
        }
        // 5 cuentas en total: páginas de 2 -> 2, 2 y 1
        get("/api/soporte/usuarios?pagina=0&tamanio=2", admin)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(2))
                .andExpect(jsonPath("$.respuesta.totalElementos").value(5))
                .andExpect(jsonPath("$.respuesta.totalPaginas").value(3));
        get("/api/soporte/usuarios?pagina=2&tamanio=2", admin)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(1))
                .andExpect(jsonPath("$.respuesta.pagina").value(2));
        get("/api/soporte/usuarios?pagina=9&tamanio=2", admin)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(0));
        // valores fuera de rango se acotan en vez de fallar
        get("/api/soporte/usuarios?pagina=-3&tamanio=100000", admin)
                .andExpect(jsonPath("$.respuesta.pagina").value(0))
                .andExpect(jsonPath("$.respuesta.tamanio").value(100))
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(5));
    }

    @Test
    void elEquipoVeLosLogrosYLosClanesParaGestionarlos() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");

        // el administrador consulta el catálogo de logros; el entrenador no
        get("/api/logros", admin).andExpect(status().isOk());
        get("/api/logros", entrenador).andExpect(status().isForbidden());
        // el equipo lista los clanes (para cerrarlos desde moderación), pero no entra a "mi clan"
        get("/api/clanes", entrenador).andExpect(status().isOk());
        get("/api/clanes", admin).andExpect(status().isOk());
        get("/api/clanes/mio", entrenador).andExpect(status().isForbidden());
        // y sigue sin poder crear ni unirse
        post("/api/clanes", Map.of("nombre", "Equipo", "capacidadMax", 5), admin).andExpect(status().isForbidden());
        get("/api/clanes", jugador).andExpect(status().isOk());
    }

    @Test
    void cadaRolSoloEntraASuZona() throws Exception {
        String jugador = registrar("leo@kinet.com", "leo_run");
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");

        // el jugador no entra a ninguna zona de staff
        get("/api/admin/retos", jugador).andExpect(status().isForbidden());
        get("/api/analitica/resumen", jugador).andExpect(status().isForbidden());
        get("/api/moderacion/entrenamientos/pendientes", jugador).andExpect(status().isForbidden());
        // el entrenador solo modera
        get("/api/moderacion/entrenamientos/pendientes", entrenador).andExpect(status().isOk());
        get("/api/admin/retos", entrenador).andExpect(status().isForbidden());
        get("/api/analitica/resumen", entrenador).andExpect(status().isForbidden());
        get("/api/soporte/usuarios", entrenador).andExpect(status().isForbidden());
        // el administrador hace todo lo del staff
        get("/api/moderacion/entrenamientos/pendientes", admin).andExpect(status().isOk());
        get("/api/admin/retos", admin).andExpect(status().isOk());
        get("/api/analitica/resumen", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.usuariosPorRol.JUGADOR").value(1));
        get("/api/soporte/usuarios?correo=leo", admin).andExpect(jsonPath("$.respuesta.contenido.length()").value(1));
        // el staff no juega
        post("/api/matchmaking/solicitudes", null, admin).andExpect(status().isForbidden());
        post("/api/matchmaking/solicitudes", null, entrenador).andExpect(status().isForbidden());
        // perfil y catálogos: cualquiera autenticado
        get("/api/catalogos/actividades", entrenador).andExpect(status().isOk());
    }

    @Test
    void elAdministradorBloqueaCuentasYAjustaXpConMotivo() throws Exception {
        registrar("leo@kinet.com", "leo_run");
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        Long idLeo = idDe("leo@kinet.com");

        post("/api/soporte/ajustes-xp", Map.of("idUsuario", idLeo, "cantidad", 50, "motivo", "Falla de sincronización"), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.origen").value("AJUSTE"));
        post("/api/soporte/ajustes-xp", Map.of("idUsuario", idLeo, "cantidad", -80, "motivo", "Corrección"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("por debajo de 0")));

        patch("/api/soporte/usuarios/" + idLeo + "/estado", Map.of("estado", "BLOQUEADO"), admin)
                .andExpect(jsonPath("$.respuesta.estado").value("BLOQUEADO"));
        post("/api/auth/login", Map.of("correo", "leo@kinet.com", "contrasena", "Segura123"), null)
                .andExpect(status().isForbidden());
        patch("/api/soporte/usuarios/" + idDe("admin@kinet.com") + "/estado", Map.of("estado", "BLOQUEADO"), admin)
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaReglaNuevaCierraLaAnteriorYNoAceptaFechasPasadas() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        Long running = idActividad("RUNNING");
        Map<String, Object> regla = Map.of("idTipoActividad", running, "intensidad", "MEDIA", "xpPorMinuto", 4.5,
                "duracionMinimaMin", 12, "vigenteDesde", LocalDate.now().plusDays(1).toString());
        post("/api/admin/reglas-puntuacion", regla, admin).andExpect(status().isCreated());
        get("/api/admin/reglas-puntuacion?idTipoActividad=" + running, admin)
                .andExpect(jsonPath("$.respuesta[?(@.intensidad=='MEDIA' && @.vigenteHasta=='"
                        + LocalDate.now() + "')].xpPorMinuto").value(3.0));

        Map<String, Object> pasada = Map.of("idTipoActividad", running, "intensidad", "ALTA", "xpPorMinuto", 5,
                "duracionMinimaMin", 10, "vigenteDesde", LocalDate.now().minusDays(1).toString());
        post("/api/admin/reglas-puntuacion", pasada, admin).andExpect(status().isBadRequest());
    }

    @Test
    void elAdministradorCambiaRolesPeroNoElSuyo() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        registrar("leo@kinet.com", "leo_run");
        patch("/api/admin/usuarios/" + idDe("leo@kinet.com") + "/rol", Map.of("rol", "ENTRENADOR"), admin)
                .andExpect(jsonPath("$.respuesta.rol").value("ENTRENADOR"));
        patch("/api/admin/usuarios/" + idDe("admin@kinet.com") + "/rol", Map.of("rol", "JUGADOR"), admin)
                .andExpect(status().isBadRequest());
    }

    @Test
    void losLogrosSeAdministranYNoSeBorranSiYaSeOtorgaron() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        post("/api/admin/logros", Map.of("nombre", "Prueba criterio", "descripcion", "x", "criterio", "RACHA=3",
                "xpBonus", 10), admin).andExpect(status().isBadRequest());
        long id = respuesta(post("/api/admin/logros", Map.of("nombre", "Prueba XP", "descripcion", "Llega a 1 XP",
                "criterio", "XP>=1", "xpBonus", 5), admin).andExpect(status().isCreated())).at("/id").asLong();

        String jugador = registrar("leo@kinet.com", "leo_run");
        entrenar(jugador, "FUERZA", 20, "BAJA", haceHoras(2), null)
                .andExpect(jsonPath("$.respuesta.logrosObtenidos.length()").value(2));
        delete("/api/admin/logros/" + id, admin).andExpect(status().isConflict());
    }
}
