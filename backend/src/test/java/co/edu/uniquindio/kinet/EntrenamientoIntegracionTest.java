package co.edu.uniquindio.kinet;

import co.edu.uniquindio.kinet.model.PerfilJugador;
import co.edu.uniquindio.kinet.repositories.PlataformaDeporteRepository;
import co.edu.uniquindio.kinet.services.EntrenamientoService;
import co.edu.uniquindio.kinet.services.ReglasJuego;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Proceso 2: cronómetro en el servidor, anti-trampa, XP según REGLA_PUNTUACION, libro de XP, racha, moderación,
 * logros y evidencia opcional.
 */
class EntrenamientoIntegracionTest extends PruebaIntegracion {

    /** PNG de 1x1. */
    private static final byte[] PNG = HexFormat.of().parseHex(
            "89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000D4944415478DA636460F85F0F"
                    + "0002870180EB47BA920000000049454E44AE426082");

    @Autowired private EntrenamientoService entrenamientoService;
    @Autowired private PlataformaDeporteRepository plataformaRepository;

    @Test
    void guardaSeriesRepeticionesYPesoDeUnaSesionDeFuerza() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        LocalDateTime inicio = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS);
        reloj.fijar(inicio);
        iniciar(token, "FUERZA").andExpect(status().isCreated());
        reloj.fijar(inicio.plusMinutes(50));

        post("/api/entrenamientos/en-curso/fin",
                Map.of("intensidad", "MEDIA", "series", 4, "repeticiones", 12, "pesoKg", 42.5), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.entrenamiento.series").value(4))
                .andExpect(jsonPath("$.respuesta.entrenamiento.repeticiones").value(12))
                .andExpect(jsonPath("$.respuesta.entrenamiento.pesoKg").value(42.5));
    }

    @Test
    void rechazaSeriesYRepeticionesFueraDeRango() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        iniciar(token, "FUERZA").andExpect(status().isCreated());

        post("/api/entrenamientos/en-curso/fin", Map.of("intensidad", "MEDIA", "series", 0), token)
                .andExpect(status().isBadRequest());
        post("/api/entrenamientos/en-curso/fin", Map.of("intensidad", "MEDIA", "repeticiones", 5000), token)
                .andExpect(status().isBadRequest());
    }

    @Test
    void elServidorMideLaDuracionEntreIniciarYFinalizar() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        LocalDateTime inicio = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS);

        reloj.fijar(inicio);
        iniciar(token, "RUNNING")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.respuesta.duracionMin").doesNotExist());
        reloj.fijar(inicio.plusMinutes(25));
        get("/api/entrenamientos/en-curso", token).andExpect(jsonPath("$.respuesta.minutosTranscurridos").value(25));
        iniciar(token, "FUERZA").andExpect(status().isConflict());

        // RUNNING MEDIA = 3 XP/min: 40 min = 120 XP, + 20 del logro "Primeros pasos"
        reloj.fijar(inicio.plusMinutes(40).plusSeconds(30));
        finalizar(token, "MEDIA", 6.5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.duracionMin").value(40))
                .andExpect(jsonPath("$.respuesta.entrenamiento.distanciaKm").value(6.5))
                .andExpect(jsonPath("$.respuesta.entrenamiento.xpOtorgado").value(120))
                .andExpect(jsonPath("$.respuesta.xpGanado").value(140))
                .andExpect(jsonPath("$.respuesta.nivel").value(2))
                .andExpect(jsonPath("$.respuesta.logrosObtenidos[0]").value("Primeros pasos"));
        reloj.liberar();
        get("/api/entrenamientos/en-curso", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").doesNotExist());

        Long id = idDe("leo@kinet.com");
        assertThat(movimientoRepository.saldo(id)).isEqualTo(140);
        assertThat(perfilRepository.findById(id).orElseThrow().getXpTotal()).isEqualTo(140);
    }

    @Test
    void laDuracionNoSePuedeEnviarDesdeElCliente() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        reloj.fijar(LocalDateTime.now().minusMinutes(20));
        iniciar(token, "FUERZA");
        reloj.liberar();
        // aunque el cliente mande una duración, el servidor usa sus propias horas (20 minutos)
        post("/api/entrenamientos/en-curso/fin", Map.of("intensidad", "MEDIA", "duracionMin", 200), token)
                .andExpect(jsonPath("$.respuesta.entrenamiento.duracionMin").value(20));
    }

    @Test
    void cancelarDescartaElCronometro() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        post("/api/entrenamientos/en-curso/fin", Map.of("intensidad", "MEDIA"), token).andExpect(status().isNotFound());
        iniciar(token, "FUERZA").andExpect(status().isCreated());
        delete("/api/entrenamientos/en-curso", token).andExpect(status().isOk());
        assertThat(entrenamientoRepository.count()).isZero();
        iniciar(token, "FUERZA").andExpect(status().isCreated());
    }

    @Test
    void elAntiTrampaRechazaYExplicaElMotivo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        // FUERZA exige 15 minutos según su regla
        entrenar(token, "FUERZA", 12, "MEDIA", haceHoras(10), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("al menos 15 minutos")))
                .andExpect(jsonPath("$.respuesta.xpGanado").value(0));
        entrenar(token, "FUERZA", 250, "MEDIA", haceHoras(9), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("Olvidaste finalizar")));
        assertThat(notificacionRepository.findAll()).anyMatch(n -> n.getTitulo().equals("Entrenamiento rechazado"));
    }

    @Test
    void elDiaTieneUnTopeDeMinutos() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        entrenar(token, "FUERZA", 170, "BAJA", hoy.minusDays(1).plusHours(6), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"));
        entrenar(token, "FUERZA", 140, "BAJA", hoy.minusDays(1).plusHours(12), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo", containsString("300 minutos")));
    }

    @Test
    void unCronometroOlvidadoSeCierraSolo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        reloj.fijar(LocalDateTime.now().minusHours(6));
        iniciar(token, "FUERZA");
        reloj.liberar();
        assertThat(entrenamientoService.cerrarAbandonados()).isEqualTo(1);
        get("/api/entrenamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.contenido[0].motivoRechazo", containsString("se cerró solo")));
    }

    @Test
    void unaSesionMuyLargaQuedaPendienteYElModeradorLaAprueba() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        // FUERZA MEDIA = 3 XP/min, 200 min = 600 XP si se aprueba
        long id = respuesta(entrenar(token, "FUERZA", 200, "MEDIA", haceHoras(5), null)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.respuesta.xpGanado").value(0))).at("/entrenamiento/id").asLong();
        // una sesión de más de 180 minutos pide el enlace de la actividad: ni una imagen ni nada sirven para aprobarla
        subirEvidencia(token, id, PNG).andExpect(status().isBadRequest());
        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", true), entrenador)
                .andExpect(status().isBadRequest());
        // el enlace se guarda, pero el bonus llega al aprobarlo
        subirEnlace(token, id, "https://www.strava.com/activities/5551110").andExpect(status().isCreated());
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isZero();

        get("/api/moderacion/entrenamientos/pendientes", token).andExpect(status().isForbidden());
        get("/api/moderacion/entrenamientos/pendientes", entrenador)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(1))
                .andExpect(jsonPath("$.respuesta.contenido[0].entrenamiento.evidencia").value("SIN_REVISAR"));
        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", true), entrenador)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.xpOtorgado").value(600));
        // 600 + 10 de la evidencia + 20 del logro
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(630);
        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", true), entrenador)
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaSesionLargaSinEnlaceSeRechazaSolaALas4Horas() throws Exception {
        String leo = registrar("leo@kinet.com", "leo_run");
        String ana = registrar("ana@kinet.com", "ana_gg");
        // 200 min que terminaron hace 5 h 40 min: ya pasaron las 4 horas para subir el enlace
        long sinEnlace = respuesta(entrenar(leo, "FUERZA", 200, "MEDIA", haceHoras(9), null)).at("/entrenamiento/id").asLong();
        // 200 min que terminaron hace 1 h 40 min: todavía hay tiempo
        long conEnlace = respuesta(entrenar(ana, "FUERZA", 200, "MEDIA", haceHoras(5), null)).at("/entrenamiento/id").asLong();

        subirEnlace(leo, sinEnlace, "https://www.strava.com/activities/6001").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("4 horas")));
        subirEnlace(ana, conEnlace, "https://www.strava.com/activities/6002").andExpect(status().isCreated());

        assertThat(entrenamientoService.rechazarSinEvidencia()).isEqualTo(1);
        get("/api/entrenamientos/" + sinEnlace, leo)
                .andExpect(jsonPath("$.respuesta.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.motivoRechazo", containsString("enlace")));
        get("/api/entrenamientos/" + conEnlace, ana).andExpect(jsonPath("$.respuesta.estado").value("PENDIENTE"));
        assertThat(entrenamientoService.rechazarSinEvidencia()).isZero();
    }

    @Test
    void unEnlaceDeUnaAppReconocidaDaElBonusYNoSeRepite() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        long otro = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(1), null)).at("/entrenamiento/id").asLong();
        long antes = movimientoRepository.saldo(idDe("leo@kinet.com"));

        // no reconocidos: sin https, dominio ajeno o parecido, ruta que no es una actividad
        subirEnlace(token, id, "http://www.strava.com/activities/123456").andExpect(status().isBadRequest());
        subirEnlace(token, id, "https://strava.com.otro.com/activities/123456").andExpect(status().isBadRequest());
        subirEnlace(token, id, "https://www.strava.com/athletes/123456").andExpect(status().isBadRequest());
        subirEnlace(token, id, "https://ejemplo.com/actividad/1").andExpect(status().isBadRequest());
        subirEnlace(token, id, "no es un enlace").andExpect(status().isBadRequest());
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(antes);

        // reconocido: se guarda sin parámetros y con la app que lo reconoció
        subirEnlace(token, id, "https://www.strava.com/activities/123456?utm_source=app")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.tipo").value("ENLACE"))
                .andExpect(jsonPath("$.respuesta.plataforma").value("STRAVA"))
                .andExpect(jsonPath("$.respuesta.url").value("https://www.strava.com/activities/123456"));
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(antes + 10);
        // una por entrenamiento, y el mismo enlace no sirve en otro entrenamiento
        subirEnlace(token, id, "https://connect.garmin.com/modern/activity/77").andExpect(status().isConflict());
        subirEnlace(token, otro, "https://www.strava.com/activities/123456").andExpect(status().isConflict());
        subirEnlace(token, otro, "https://connect.garmin.com/modern/activity/77").andExpect(status().isCreated());
        // un enlace no tiene imagen que descargar
        get("/api/entrenamientos/" + id + "/evidencia", token).andExpect(status().isNotFound());
        get("/api/entrenamientos/" + id, token).andExpect(jsonPath("$.respuesta.evidencia").value("SIN_REVISAR"));
    }

    @Test
    void unaAppApagadaEnLaTablaDejaDeAceptarseSinTocarElCodigo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        var komoot = plataformaRepository.findAll().stream()
                .filter(p -> p.getDominio().equals("komoot.com")).findFirst().orElseThrow();
        try {
            komoot.setActiva(false);
            plataformaRepository.save(komoot);
            subirEnlace(token, id, "https://www.komoot.com/tour/998877").andExpect(status().isBadRequest());
        } finally {
            komoot.setActiva(true);
            plataformaRepository.save(komoot);
        }
        subirEnlace(token, id, "https://www.komoot.com/tour/998877")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.plataforma").value("KOMOOT"));
    }

    @Test
    void unaSesionLargaConEnlaceRechazadoNoSePuedeAprobar() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        long id = respuesta(entrenar(token, "FUERZA", 200, "MEDIA", haceHoras(5), null)).at("/entrenamiento/id").asLong();
        long idEvidencia = respuesta(subirEnlace(token, id, "https://www.strava.com/activities/31415")).at("/id").asLong();
        post("/api/moderacion/evidencias/" + idEvidencia + "/revision",
                Map.of("aceptar", false, "nota", "No es de esa sesión"), entrenador).andExpect(status().isOk());

        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", true), entrenador)
                .andExpect(status().isBadRequest());
        // sin enlace vigente, y pasadas las 4 horas, la sesión se rechaza sola
        reloj.fijar(LocalDateTime.now().plusHours(5));
        try {
            assertThat(entrenamientoService.rechazarSinEvidencia()).isEqualTo(1);
        } finally {
            reloj.liberar();
        }
        get("/api/entrenamientos/" + id, token).andExpect(jsonPath("$.respuesta.estado").value("RECHAZADO"));
    }

    @Test
    void elEnlaceReemplazaUnaImagenSinRepetirElBonus() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        subirEvidencia(token, id, PNG).andExpect(status().isCreated());
        long conImagen = movimientoRepository.saldo(idDe("leo@kinet.com"));

        subirEnlace(token, id, "https://flow.polar.com/training/analysis/4455")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.tipo").value("ENLACE"))
                .andExpect(jsonPath("$.respuesta.plataforma").value("POLAR FLOW"));
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(conImagen);
        assertThat(evidenciaRepository.findByEntrenamientoId(id).orElseThrow().esEnlace()).isTrue();
    }

    @Test
    void rechazarUnPendienteExigeMotivo() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        long id = respuesta(entrenar(token, "FUERZA", 190, "BAJA", haceHoras(4), null)).at("/entrenamiento/id").asLong();
        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", false), entrenador)
                .andExpect(status().isBadRequest());
        post("/api/moderacion/entrenamientos/" + id + "/revision", Map.of("aprobar", false, "motivo", "Duración irreal"),
                entrenador)
                .andExpect(jsonPath("$.respuesta.entrenamiento.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.entrenamiento.motivoRechazo").value("Duración irreal"));
    }

    @Test
    void laEvidenciaEsOpcionalYDaUnBonus() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        long antes = movimientoRepository.saldo(idDe("leo@kinet.com"));

        subirEvidencia(token, id, "no soy una imagen".getBytes()).andExpect(status().isBadRequest());
        subirEvidencia(token, id, PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.estadoRevision").value("SIN_REVISAR"))
                .andExpect(jsonPath("$.respuesta.tipoContenido").value("image/png"));
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(antes + 10);
        subirEvidencia(token, id, PNG).andExpect(status().isConflict());

        mvc.perform(MockMvcRequestBuilders.get("/api/entrenamientos/" + id + "/evidencia")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
        get("/api/entrenamientos/" + id, token).andExpect(jsonPath("$.respuesta.evidencia").value("SIN_REVISAR"));
        get("/api/progreso/movimientos", token).andExpect(jsonPath("$.respuesta.contenido[0].origen").value("EVIDENCIA"));

        String otro = registrar("ana@kinet.com", "ana_gg");
        subirEvidencia(otro, id, PNG).andExpect(status().isNotFound());
    }

    @Test
    void laEvidenciaSeValidaPorSuContenidoYSuTamano() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();

        subirEvidencia(token, id, new byte[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("Adjunta una imagen."));
        byte[] grande = Arrays.copyOf(PNG, ReglasJuego.TAMANO_MAXIMO_EVIDENCIA + 1);
        subirEvidencia(token, id, grande)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta").value("La imagen no puede pesar más de 5 MB."));
        assertThat(evidenciaRepository.findAll()).isEmpty();

        // el archivo se llama captura.png, pero el tipo se decide por sus primeros bytes: es un JPEG
        byte[] jpeg = HexFormat.of().parseHex("FFD8FFE000104A46494600010100000100010000FFD9");
        subirEvidencia(token, id, jpeg)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respuesta.tipoContenido").value("image/jpeg"));
    }

    @Test
    void siElModeradorRechazaLaEvidenciaSeRevierteElBonus() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        String entrenador = cuentaConRol("entrenador@kinet.com", "entrenador_k", "ENTRENADOR");
        long id = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        long idEvidencia = respuesta(subirEvidencia(token, id, PNG)).at("/id").asLong();
        long conBonus = movimientoRepository.saldo(idDe("leo@kinet.com"));

        get("/api/moderacion/evidencias", entrenador)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(1))
                .andExpect(jsonPath("$.respuesta.contenido[0].gamertag").value("leo_run"));
        mvc.perform(MockMvcRequestBuilders.get("/api/moderacion/evidencias/" + idEvidencia + "/archivo")
                        .header("Authorization", "Bearer " + entrenador))
                .andExpect(content().bytes(PNG));
        post("/api/moderacion/evidencias/" + idEvidencia + "/revision", Map.of("aceptar", false), entrenador)
                .andExpect(status().isBadRequest());
        post("/api/moderacion/evidencias/" + idEvidencia + "/revision",
                Map.of("aceptar", false, "nota", "La captura es de otro día"), entrenador)
                .andExpect(jsonPath("$.respuesta.estadoRevision").value("RECHAZADA"));
        assertThat(movimientoRepository.saldo(idDe("leo@kinet.com"))).isEqualTo(conBonus - 10);
        get("/api/moderacion/evidencias", entrenador).andExpect(jsonPath("$.respuesta.contenido.length()").value(0));
    }

    @Test
    void laEvidenciaSoloSeSubeDeEntrenamientosTerminadosYRecientes() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        long rechazado = respuesta(entrenar(token, "FUERZA", 5, "MEDIA", haceHoras(3), null)).at("/entrenamiento/id").asLong();
        subirEvidencia(token, rechazado, PNG).andExpect(status().isBadRequest());

        long viejo = respuesta(entrenar(token, "FUERZA", 30, "MEDIA", haceHoras(60), null)).at("/entrenamiento/id").asLong();
        subirEvidencia(token, viejo, PNG)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.respuesta", containsString("48 horas")));
    }

    @Test
    void laRachaCuentaDiasSeguidosYDesbloqueaElLogro() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        PerfilJugador perfil = perfilRepository.findById(idDe("leo@kinet.com")).orElseThrow();
        perfil.setRachaActual(2);
        perfil.setRachaMaxima(2);
        perfil.setFechaUltimaActividad(LocalDate.now().minusDays(1));
        perfilRepository.save(perfil);

        entrenar(token, "CALISTENIA", 20, "MEDIA", LocalDateTime.now().minusMinutes(30), null)
                .andExpect(jsonPath("$.respuesta.rachaActual").value(3))
                .andExpect(jsonPath("$.respuesta.logrosObtenidos.length()").value(2));
    }

    @Test
    void elHistorialMuestraElXpDeCadaEntrenamiento() throws Exception {
        String token = registrar("leo@kinet.com", "leo_run");
        LocalDateTime ayer = LocalDate.now().minusDays(1).atStartOfDay();
        entrenar(token, "CICLISMO", 60, "ALTA", ayer.plusHours(8), 25.0); // 60 x 3 = 180
        entrenar(token, "FUERZA", 5, "BAJA", ayer.plusHours(12), null);   // rechazado
        get("/api/entrenamientos", token)
                .andExpect(jsonPath("$.respuesta.contenido.length()").value(2))
                .andExpect(jsonPath("$.respuesta.contenido[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.respuesta.contenido[0].xpOtorgado").value(0))
                .andExpect(jsonPath("$.respuesta.contenido[1].xpOtorgado").value(180));

        String otro = registrar("ana@kinet.com", "ana_gg");
        long idEntrenamiento = entrenamientoRepository.findAll().get(0).getId();
        get("/api/entrenamientos/" + idEntrenamiento, otro).andExpect(status().isNotFound());
    }

    private ResultActions subirEvidencia(String token, long idEntrenamiento, byte[] contenido) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.multipart("/api/entrenamientos/" + idEntrenamiento + "/evidencia")
                .file(new MockMultipartFile("archivo", "captura.png", "image/png", contenido))
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void unaActividadInactivaNoSeMuestraNiSePuedeUsar() throws Exception {
        String admin = cuentaConRol("admin@kinet.com", "admin_k", "ADMINISTRADOR");
        String leo = registrar("leo@kinet.com", "leo_run");
        String url = "/api/admin/actividades/" + idActividad("RUNNING") + "/estado";
        patch(url, Map.of("estado", "INACTIVA"), leo).andExpect(status().isForbidden());
        patch(url, Map.of("estado", "INACTIVA"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta.estado").value("INACTIVA"));

        get("/api/catalogos/actividades", leo)
                .andExpect(jsonPath("$.respuesta[?(@.nombre=='RUNNING')]").isEmpty());
        get("/api/admin/actividades", admin)
                .andExpect(jsonPath("$.respuesta[?(@.nombre=='RUNNING')].estado").value("INACTIVA"));
        iniciar(leo, "RUNNING").andExpect(status().isBadRequest());

        patch(url, Map.of("estado", "ACTIVA"), admin).andExpect(status().isOk());
        iniciar(leo, "RUNNING").andExpect(status().isCreated());
    }
}
