package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Competencia;
import co.edu.uniquindio.kinet.model.enums.OrigenXp;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Competencias por invitación: duelo, grupal y torneo; aceptar, rechazar, vencer, rondas y premios. */
class CompetenciaIntegracionTest extends PruebaIntegracion {

    @Test
    void enUnaCompetenciaSoloSumaLoQueTieneEnlaceDeLaActividad() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        long id = id(crear(leo, "DUELO", "Duelo con evidencia", List.of(idDe("ana@kinet.com")), null, 4, null)
                .andExpect(status().isCreated()));
        post("/api/competencias/" + id + "/aceptacion", null, ana).andExpect(status().isOk());
        ventana(id, LocalDateTime.now().minusHours(3), LocalDateTime.now().plusHours(1));

        entrenarConEvidencia(leo, "FUERZA", 40, "MEDIA", haceHoras(2), null); // 120 XP con enlace
        entrenar(ana, "FUERZA", 20, "MEDIA", haceHoras(2), null);             // 60 XP sin enlace: no cuenta
        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].puntaje").value(120.0))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].puntaje").value(0.0));
    }

    @Test
    void unDueloEmpiezaAlAceptarYGanaQuienSumaMasXp() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        long id = id(crear(leo, "DUELO", "Duelo de prueba", List.of(idDe("ana@kinet.com")), null, 4, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].estadoInvitacion").value("INVITADO")));

        get("/api/competencias/invitaciones", ana).andExpect(jsonPath("$.respuesta.contenido.length()").value(1));
        post("/api/competencias/" + id + "/aceptacion", null, ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.respuesta.rondaActual").value(1));
        post("/api/competencias/" + id + "/aceptacion", null, ana).andExpect(status().isConflict());

        // la ronda empezó hace 3 horas y termina en 1
        ventana(id, LocalDateTime.now().minusHours(3), LocalDateTime.now().plusHours(1));
        entrenarConEvidencia(leo, "FUERZA", 40, "MEDIA", haceHoras(2), null); // 120 XP
        entrenarConEvidencia(ana, "FUERZA", 20, "MEDIA", haceHoras(2), null); // 60 XP
        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].puntaje").value(120.0));

        terminar(id);
        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].resultado").value("GANADOR"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].posicion").value(1))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].resultado").value("PERDEDOR"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].posicion").value(2));
        assertThat(premios()).containsExactly(Map.entry("leo@kinet.com", 30));
    }

    @Test
    void unDueloRechazadoSeCancela() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        long id = id(crear(leo, "DUELO", "Duelo", List.of(idDe("ana@kinet.com")), null, 4, null));
        post("/api/competencias/" + id + "/rechazo", null, ana)
                .andExpect(jsonPath("$.respuesta.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].estadoInvitacion").value("RECHAZADO"));
        get("/api/competencias/invitaciones", ana).andExpect(jsonPath("$.respuesta.contenido.length()").value(0));
        post("/api/competencias/" + id + "/aceptacion", null, ana).andExpect(status().isConflict());
        // rechazada: no aparece entre las competencias de quien la rechazó
        get("/api/competencias", ana).andExpect(jsonPath("$.respuesta.contenido.length()").value(0));
    }

    @Test
    void unDueloSinXpTerminaEnEmpateSinPremio() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        long id = id(crear(leo, "DUELO", "Duelo", List.of(idDe("ana@kinet.com")), null, 4, null));
        post("/api/competencias/" + id + "/aceptacion", null, ana);
        terminar(id);
        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.respuesta.participantes[0].resultado").value("EMPATE"))
                .andExpect(jsonPath("$.respuesta.participantes[1].resultado").value("EMPATE"));
        assertThat(premios()).isEmpty();
    }

    @Test
    void unaGrupalSinSuficientesJugadoresSeCancelaYLasInvitacionesVencen() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        registrar("sofi@kinet.com", "sofi_fit");
        long id = id(crear(leo, "GRUPAL", "Reto del parque", List.of(idDe("ana@kinet.com"), idDe("sofi@kinet.com")),
                null, 24, LocalDateTime.now().plusHours(2)).andExpect(status().isCreated()));
        post("/api/competencias/" + id + "/rechazo", null, ana)
                .andExpect(jsonPath("$.respuesta.estado").value("PENDIENTE"));

        Competencia c = competenciaRepository.findById(id).orElseThrow();
        c.setFechaInicio(LocalDateTime.now().minusMinutes(1));
        competenciaRepository.save(c);
        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='sofi_fit')].estadoInvitacion").value("VENCIDA"));
    }

    @Test
    void unTorneoEliminaALaMitadPorRondaYPremiaAlCampeon() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        String sofi = registrar("sofi@kinet.com", "sofi_fit");
        String max = registrar("max@kinet.com", "max_pro");
        long id = id(crear(leo, "TORNEO", "Copa de prueba", List.of(idDe("ana@kinet.com"), idDe("sofi@kinet.com"),
                idDe("max@kinet.com")), idActividad("FUERZA"), 4, LocalDateTime.now().plusHours(1)));
        for (String token : List.of(ana, sofi, max)) {
            post("/api/competencias/" + id + "/aceptacion", null, token)
                    .andExpect(jsonPath("$.respuesta.estado").value("PENDIENTE"));
        }
        // empezó hace 10 horas: ronda 1 = [-10 h, -6 h), ronda 2 (final) = [-6 h, -2 h)
        Competencia c = competenciaRepository.findById(id).orElseThrow();
        c.setFechaInicio(LocalDateTime.now().minusHours(10));
        competenciaRepository.save(c);

        entrenarConEvidencia(leo, "FUERZA", 40, "MEDIA", haceHoras(9), null);  // 120
        entrenarConEvidencia(ana, "FUERZA", 20, "MEDIA", haceHoras(9), null);  // 60
        entrenarConEvidencia(sofi, "FUERZA", 15, "MEDIA", haceHoras(9), null); // 45
        entrenarConEvidencia(max, "CICLISMO", 60, "ALTA", haceHoras(9), null); // otra actividad: no cuenta
        entrenarConEvidencia(ana, "FUERZA", 40, "MEDIA", haceHoras(5), null);  // 120 en la final
        entrenarConEvidencia(leo, "FUERZA", 20, "MEDIA", haceHoras(5), null);  // 60 en la final

        get("/api/competencias/" + id, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.respuesta.rondaActual").value(2))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].resultado").value("GANADOR"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].posicion").value(1))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].posicion").value(2))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='sofi_fit')].eliminadoEnRonda").value(1))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='sofi_fit')].posicion").value(3))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='max_pro')].posicion").value(4))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='max_pro')].puntaje").value(0.0));
        assertThat(premios()).containsExactly(Map.entry("ana@kinet.com", 100));
    }

    @Test
    void reglasAlCrear() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String leo = registrar("leo@kinet.com", "leo_run");
        registrar("ana@kinet.com", "ana_gg");
        registrar("sofi@kinet.com", "sofi_fit");
        Long ana = idDe("ana@kinet.com");
        Long sofi = idDe("sofi@kinet.com");
        LocalDateTime manana = LocalDateTime.now().plusDays(1);

        crear(leo, "DUELO", "Duelo", List.of(ana, sofi), null, 4, null).andExpect(status().isBadRequest());
        crear(leo, "DUELO", "Duelo", List.of(idDe("leo@kinet.com")), null, 4, null).andExpect(status().isBadRequest());
        crear(leo, "GRUPAL", "Grupal", List.of(ana, sofi), null, 4, null).andExpect(status().isBadRequest());
        crear(leo, "GRUPAL", "Grupal", List.of(ana, sofi), null, 4, LocalDateTime.now().plusDays(8))
                .andExpect(status().isBadRequest());
        crear(leo, "TORNEO", "Torneo", List.of(ana, sofi), null, 4, manana).andExpect(status().isBadRequest());
        crear(leo, "DUELO", "Duelo", List.of(idDe("admin@kinet.com")), null, 4, null).andExpect(status().isBadRequest());
        crear(leo, "DUELO", "Duelo", List.of(ana), null, 200, null).andExpect(status().isBadRequest());

        patch("/api/admin/actividades/" + idActividad("RUNNING") + "/estado", Map.of("estado", "INACTIVA"), admin)
                .andExpect(status().isOk());
        crear(leo, "DUELO", "Duelo", List.of(ana), idActividad("RUNNING"), 4, null).andExpect(status().isBadRequest());

        // máximo 3 competencias pendientes o en curso
        for (int i = 0; i < 3; i++) {
            crear(leo, "GRUPAL", "Grupal " + i, List.of(ana, sofi), null, 4, manana).andExpect(status().isCreated());
        }
        crear(leo, "GRUPAL", "Una más", List.of(ana, sofi), null, 4, manana).andExpect(status().isConflict());
    }

    @Test
    void soloElCreadorCancelaYSoloSiNoHaEmpezado() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        String otro = registrar("otro@kinet.com", "otro_x");
        long id = id(crear(leo, "DUELO", "Duelo", List.of(idDe("ana@kinet.com")), null, 4, null));
        post("/api/competencias/" + id + "/cancelacion", null, ana).andExpect(status().isForbidden());
        get("/api/competencias/" + id, otro).andExpect(status().isNotFound());
        post("/api/competencias/" + id + "/cancelacion", null, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("CANCELADA"));
        post("/api/competencias/" + id + "/cancelacion", null, leo).andExpect(status().isBadRequest());
    }

    // ---------- apoyo ----------

    private ResultActions crear(String token, String tipo, String nombre,
                                List<Long> invitados, Long idActividad, int horas, LocalDateTime inicio) throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("tipo", tipo);
        datos.put("nombre", nombre);
        datos.put("idsInvitados", invitados);
        datos.put("idTipoActividad", idActividad);
        datos.put("duracionRondaHoras", horas);
        datos.put("fechaInicio", inicio == null ? null : inicio.withNano(0).toString());
        return post("/api/competencias", datos, token);
    }

    private long id(ResultActions resultado) throws Exception {
        return respuesta(resultado).at("/id").asLong();
    }

    private void ventana(long id, LocalDateTime inicio, LocalDateTime fin) {
        Competencia c = competenciaRepository.findById(id).orElseThrow();
        c.setFechaInicio(inicio);
        c.setFechaFin(fin);
        c.setDuracionRondaHoras((int) Duration.between(inicio, fin).toHours());
        competenciaRepository.save(c);
    }

    /** Hace que la ronda en curso haya terminado hace un minuto. */
    private void terminar(long id) {
        Competencia c = competenciaRepository.findById(id).orElseThrow();
        LocalDateTime fin = LocalDateTime.now().minusMinutes(1);
        c.setFechaFin(fin);
        c.setFechaInicio(fin.minusHours(c.getDuracionRondaHoras()));
        competenciaRepository.save(c);
    }

    /** Premios de competencia entregados: correo -> XP. */
    private Map<String, Integer> premios() {
        Map<String, Integer> premios = new HashMap<>();
        movimientoRepository.findAll().stream().filter(m -> m.getOrigen() == OrigenXp.COMPETENCIA)
                .forEach(m -> premios.put(usuarioRepository.findById(m.getUsuario().getId()).orElseThrow().getCorreo(),
                        m.getCantidad()));
        return premios;
    }
}
