package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.Enfrentamiento;
import co.edu.uniquindio.kinet.model.ParticipacionTemporada;
import co.edu.uniquindio.kinet.model.Temporada;
import co.edu.uniquindio.kinet.model.enums.EstadoTemporada;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Temporadas, colocación, emparejamiento, resultados, puntos, rangos y cierre. */
class CompetitivoIntegracionTest extends PruebaIntegracion {

    @Test
    void laPrimeraSolicitudEsUnaColocacionIndividual() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/matchmaking/solicitudes", null, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("EMPAREJADA"))
                .andExpect(jsonPath("$.respuesta.enfrentamiento.tipo").value("COLOCACION"))
                .andExpect(jsonPath("$.respuesta.enfrentamiento.participantes.length()").value(1));
        post("/api/matchmaking/solicitudes", null, token).andExpect(status().isConflict());
        get("/api/temporadas/activa/mia", token)
                .andExpect(jsonPath("$.respuesta.puntosActuales").value(0))
                .andExpect(jsonPath("$.respuesta.rangoActual.nombre").value("BRONCE III"));
    }

    @Test
    void sinElEnlaceDeLaActividadElEntrenamientoNoSumaEnElEnfrentamiento() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/matchmaking/solicitudes", null, token);
        mover(enfrentamientoRepository.findAll().get(0), 5);
        long id = respuesta(entrenar(token, "FUERZA", 60, "ALTA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        get("/api/matchmaking/enfrentamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].puntaje").value(0.0));
        // competir exige el enlace de una app deportiva reconocida: sin él la sesión no suma
        subirEnlace(token, id, "https://www.strava.com/activities/8800123").andExpect(status().isCreated());
        get("/api/matchmaking/enfrentamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].puntaje").value(240.0));
    }

    @Test
    void siElEntrenadorRechazaElEnlaceLaSesionDejaDeSumar() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        post("/api/matchmaking/solicitudes", null, token);
        mover(enfrentamientoRepository.findAll().get(0), 5);
        long id = respuesta(entrenarConEvidencia(token, "FUERZA", 60, "ALTA", haceHoras(3), null))
                .at("/entrenamiento/id").asLong();
        get("/api/matchmaking/enfrentamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].puntaje").value(240.0));

        long idEvidencia = evidenciaRepository.findByEntrenamientoId(id).orElseThrow().getId();
        post("/api/moderacion/evidencias/" + idEvidencia + "/revision",
                Map.of("aceptar", false, "nota", "El enlace no corresponde a esa sesión"), entrenador)
                .andExpect(status().isOk());
        get("/api/matchmaking/enfrentamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].puntaje").value(0.0));
    }

    @Test
    void laColocacionDaUnTercioDelXpDeLaVentana() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/matchmaking/solicitudes", null, token);
        mover(enfrentamientoRepository.findAll().get(0), 5);
        // FUERZA ALTA = 4 XP/min: 60 min = 240 XP dentro de la ventana -> 80 puntos
        entrenarConEvidencia(token, "FUERZA", 60, "ALTA", haceHoras(3), null);
        terminar(enfrentamientoRepository.findAll().get(0));

        get("/api/matchmaking/enfrentamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].puntaje").value(240.0))
                .andExpect(jsonPath("$.respuesta.contenido[0].participantes[0].resultado").value("COMPLETADO"));
        get("/api/temporadas/activa/mia", token).andExpect(jsonPath("$.respuesta.puntosActuales").value(80));
    }

    @Test
    void dosJugadoresEnColaSeEmparejanYGanaQuienSumaMasXp() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        colocacionTerminada(leo);
        colocacionTerminada(ana);

        post("/api/matchmaking/solicitudes", null, leo).andExpect(jsonPath("$.respuesta.estado").value("EN_COLA"));
        post("/api/matchmaking/solicitudes", null, ana)
                .andExpect(jsonPath("$.respuesta.estado").value("EMPAREJADA"))
                .andExpect(jsonPath("$.respuesta.enfrentamiento.tipo").value("ESTANDAR"))
                .andExpect(jsonPath("$.respuesta.enfrentamiento.participantes.length()").value(2));

        Enfrentamiento duelo = enfrentamientoRepository.findAll().stream()
                .filter(e -> e.getParticipantes().size() == 2).findFirst().orElseThrow();
        mover(duelo, 5);
        entrenarConEvidencia(leo, "FUERZA", 40, "MEDIA", haceHoras(3), null); // 120 XP
        entrenarConEvidencia(ana, "FUERZA", 20, "MEDIA", haceHoras(3), null); // 60 XP
        terminar(duelo);

        get("/api/matchmaking/enfrentamientos/" + duelo.getId(), leo)
                .andExpect(jsonPath("$.respuesta.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='leo_run')].resultado").value("VICTORIA"))
                .andExpect(jsonPath("$.respuesta.participantes[?(@.gamertag=='ana_gg')].resultado").value("DERROTA"));
        get("/api/temporadas/activa/mia", leo).andExpect(jsonPath("$.respuesta.puntosActuales").value(25));
        get("/api/temporadas/activa/mia", ana).andExpect(jsonPath("$.respuesta.puntosActuales").value(0));
        get("/api/logros", leo).andExpect(jsonPath("$.respuesta[?(@.nombre=='Primera victoria')].obtenido").value(true));
        get("/api/temporadas/activa/ranking", leo).andExpect(jsonPath("$.respuesta[0].gamertag").value("leo_run"));
    }

    @Test
    void sePuedeSalirDeLaCola() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        colocacionTerminada(leo);
        post("/api/matchmaking/solicitudes", null, leo).andExpect(jsonPath("$.respuesta.estado").value("EN_COLA"));
        delete("/api/matchmaking/solicitudes/actual", leo).andExpect(status().isOk());
        delete("/api/matchmaking/solicitudes/actual", leo).andExpect(status().isNotFound());
    }

    @Test
    void alCerrarLaTemporadaSeGuardaLaFotoYLaSiguienteConservaElPorcentaje() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String leo = registrar("leo@kinet.com", "leo_run");
        post("/api/matchmaking/solicitudes", null, leo);
        ParticipacionTemporada p = participacionTemporadaRepository.findAll().get(0);
        p.setPuntosActuales(450);
        participacionTemporadaRepository.save(p);

        Temporada actual = temporadaRepository.findFirstByEstado(EstadoTemporada.ACTIVA).orElseThrow();
        long nueva = respuesta(post("/api/admin/temporadas", Map.of("nombre", "Temporada siguiente",
                "fechaInicio", LocalDate.now().toString(), "fechaFin", LocalDate.now().plusDays(60).toString(),
                "porcentajeConservado", 40), admin).andExpect(status().isCreated())).at("/id").asLong();
        post("/api/admin/temporadas/" + nueva + "/activacion", null, admin).andExpect(status().isConflict());

        post("/api/admin/temporadas/" + actual.getId() + "/cierre", null, admin)
                .andExpect(jsonPath("$.respuesta.estado").value("CERRADA"));
        ParticipacionTemporada cerrada = participacionTemporadaRepository.findAll().get(0);
        assertThat(cerrada.getPuntosFinales()).isEqualTo(450);
        assertThat(cerrada.getPosicionFinal()).isEqualTo(1);
        assertThat(enfrentamientoRepository.findAll().get(0).getEstado().name()).isEqualTo("CANCELADO");

        post("/api/admin/temporadas/" + nueva + "/activacion", null, admin).andExpect(status().isOk());
        post("/api/matchmaking/solicitudes", null, leo).andExpect(status().isCreated());
        // la temporada cerrada conservaba el 50 %: 450 x 50 % = 225 (BRONCE I)
        get("/api/temporadas/activa/mia", leo)
                .andExpect(jsonPath("$.respuesta.puntosIniciales").value(225))
                .andExpect(jsonPath("$.respuesta.rangoInicial.nombre").value("BRONCE I"));
    }

    @Test
    void cadaCambioDeRangoQuedaEnElHistorial() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        post("/api/matchmaking/solicitudes", null, leo);
        mover(enfrentamientoRepository.findAll().get(0), 5);
        // FUERZA ALTA: 75 min = 300 XP -> 100 puntos: de BRONCE III a BRONCE II
        entrenarConEvidencia(leo, "FUERZA", 75, "ALTA", haceHoras(3), null);
        terminar(enfrentamientoRepository.findAll().get(0));
        get("/api/matchmaking/enfrentamientos", leo);

        get("/api/temporadas/activa/mia/historial-rango", leo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(2))
                .andExpect(jsonPath("$.respuesta.contenido[0].causa").value("INSCRIPCION"))
                .andExpect(jsonPath("$.respuesta.contenido[0].rangoAnterior").doesNotExist())
                .andExpect(jsonPath("$.respuesta.contenido[0].rangoNuevo").value("BRONCE III"))
                .andExpect(jsonPath("$.respuesta.contenido[1].causa").value("ENFRENTAMIENTO"))
                .andExpect(jsonPath("$.respuesta.contenido[1].rangoAnterior").value("BRONCE III"))
                .andExpect(jsonPath("$.respuesta.contenido[1].rangoNuevo").value("BRONCE II"))
                .andExpect(jsonPath("$.respuesta.contenido[1].puntos").value(100));
    }

    @Test
    void quienAbandonaSaleDeLaColaYDelRankingYNoPuedeVolver() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        colocacionTerminada(leo);
        colocacionTerminada(ana);
        post("/api/matchmaking/solicitudes", null, leo).andExpect(jsonPath("$.respuesta.estado").value("EN_COLA"));

        post("/api/temporadas/activa/abandono", null, leo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("ABANDONADA"))
                .andExpect(jsonPath("$.respuesta.posicion").doesNotExist());
        post("/api/temporadas/activa/abandono", null, leo).andExpect(status().isBadRequest());
        delete("/api/matchmaking/solicitudes/actual", leo).andExpect(status().isNotFound());
        post("/api/matchmaking/solicitudes", null, leo).andExpect(status().isBadRequest());
        // ana ya no encuentra a leo en la cola
        post("/api/matchmaking/solicitudes", null, ana).andExpect(jsonPath("$.respuesta.estado").value("EN_COLA"));
        get("/api/temporadas/activa/ranking", ana)
                .andExpect(jsonPath("$.respuesta.length()").value(1))
                .andExpect(jsonPath("$.respuesta[0].gamertag").value("ana_gg"));
    }

    @Test
    void elDescalificadoQuedaSinPuntosFinalesAlCerrar() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        colocacionTerminada(leo);
        colocacionTerminada(ana);
        String url = "/api/moderacion/temporadas/activa/participantes/" + idDe("leo@kinet.com") + "/descalificacion";

        post(url, Map.of("motivo", "Entrenamientos falsos"), ana).andExpect(status().isForbidden());
        post(url, Map.of("motivo", ""), entrenador).andExpect(status().isBadRequest());
        post(url, Map.of("motivo", "Entrenamientos falsos"), entrenador).andExpect(status().isOk());
        post(url, Map.of("motivo", "Otra vez"), entrenador).andExpect(status().isConflict());
        get("/api/temporadas/activa/mia", leo).andExpect(jsonPath("$.respuesta.estado").value("DESCALIFICADA"));

        Temporada actual = temporadaRepository.findFirstByEstado(EstadoTemporada.ACTIVA).orElseThrow();
        post("/api/admin/temporadas/" + actual.getId() + "/cierre", null, admin).andExpect(status().isOk());
        ParticipacionTemporada deLeo = participacionTemporadaRepository
                .findByUsuarioIdAndTemporadaId(idDe("leo@kinet.com"), actual.getId()).orElseThrow();
        ParticipacionTemporada deAna = participacionTemporadaRepository
                .findByUsuarioIdAndTemporadaId(idDe("ana@kinet.com"), actual.getId()).orElseThrow();
        assertThat(deLeo.getEstado().name()).isEqualTo("DESCALIFICADA");
        assertThat(deLeo.getPuntosFinales()).isNull();
        assertThat(deLeo.getPosicionFinal()).isNull();
        assertThat(deAna.getEstado().name()).isEqualTo("FINALIZADA");
        assertThat(deAna.getPosicionFinal()).isEqualTo(1);
    }

    /** Hace y termina la colocación del jugador, sin entrenar (0 puntos). */
    private void colocacionTerminada(String token) throws Exception {
        long id = respuesta(post("/api/matchmaking/solicitudes", null, token)).at("/enfrentamiento/id").asLong();
        terminar(enfrentamientoRepository.findById(id).orElseThrow());
        get("/api/matchmaking/enfrentamientos", token);
    }

    /** Corre la ventana del enfrentamiento para que haya empezado hace "horas" horas. */
    private void mover(Enfrentamiento e, int horas) {
        e.setFechaInicio(LocalDateTime.now().minusHours(horas));
        e.setFechaFin(e.getFechaInicio().plusHours(24));
        enfrentamientoRepository.save(e);
    }

    private void terminar(Enfrentamiento e) {
        Enfrentamiento actual = enfrentamientoRepository.findById(e.getId()).orElseThrow();
        actual.setFechaFin(LocalDateTime.now().minusMinutes(1));
        if (actual.getFechaInicio().isAfter(actual.getFechaFin())) {
            actual.setFechaInicio(actual.getFechaFin().minusHours(24));
        }
        enfrentamientoRepository.save(actual);
    }
}
